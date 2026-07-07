package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.core.worker.domain.Battle
import com.imstargg.core.worker.domain.Player
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityEvent
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntityBrawler
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleTeamPlayerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheStore
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class PlayerUpdater(
    private val playerRepository: PlayerRepository,
    private val battleJpaRepository: BattleJpaRepository,
    private val battleTeamPlayerJpaRepository: BattleTeamPlayerJpaRepository,
    private val playerBrawlerStatsCacheStore: PlayerBrawlerStatsCacheStore,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PlayerUpdater::class.java)
    }

    /**
     * 신규 플레이어 동시 INSERT race(유니크 충돌) 는 1회 재시도로 흡수한다. Spring Retry
     * 인터셉터가 트랜잭션 인터셉터 바깥에 적용되어 재시도마다 새 트랜잭션이 열리고,
     * 재시도 시점에는 row 가 존재해 SELECT FOR UPDATE 가 정상 동작한다.
     */
    @Retryable(retryFor = [DataIntegrityViolationException::class], maxAttempts = 2)
    @Transactional
    fun update(
        playerResponse: PlayerResponse,
        battles: List<Battle>,
    ): List<Battle> {
        val player = findOrCreate(playerResponse)
        player.update(playerResponse)
        playerRepository.save(player)
        val newBattles = filterNewBattles(player.entity, battles)
        log.debug("[플레이어 갱신] 신규 배틀 {}건 tag={}", newBattles.size, playerResponse.tag)
        updateBattles(player.entity, newBattles)
        if (newBattles.isNotEmpty()) {
            evictBrawlerStatsCacheAfterCommit(player.entity.brawlStarsTag)
        }
        return newBattles
    }

    @Transactional
    fun updatePlayer(response: PlayerResponse) {
        val player = findOrCreate(response)
        player.update(response)
        playerRepository.save(player)
    }

    /**
     * 신규 배틀 반영 후 그 플레이어의 브롤러 통계 캐시를 무효화한다.
     * 커밋 전에 DEL 하면 동시 읽기가 신규 배틀 미반영 상태로 재적재할 수 있어 afterCommit 으로 순서를 보장한다.
     */
    private fun evictBrawlerStatsCacheAfterCommit(playerTag: String) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        playerBrawlerStatsCacheStore.evict(playerTag)
                    }
                },
            )
        } else {
            playerBrawlerStatsCacheStore.evict(playerTag)
        }
    }

    /**
     * 최신 저장 배틀과 같은 초(경계)의 배틀도 후보에 포함하고, battleKey 로 이미 저장된
     * 것을 제외한다. battleAt 단순 비교(>)만으로는 같은 초에 걸친 미저장 배틀이 누락된다.
     */
    private fun filterNewBattles(
        player: PlayerEntity,
        battles: List<Battle>,
    ): List<Battle> {
        val latestBattleTime =
            battleJpaRepository
                .findTopByPlayerIdOrderByBattleAtDesc(player.id!!)
                ?.battleAt
                ?: return battles
        val candidates = battles.filter { !it.battleAt.isBefore(latestBattleTime) }
        if (candidates.isEmpty()) {
            return emptyList()
        }
        val existingKeys =
            battleJpaRepository
                .findAllByPlayerIdAndBattleAtGreaterThanEqual(player.id!!, latestBattleTime)
                .map { it.battleKey }
                .toSet()
        return candidates.filter { it.battleKey !in existingKeys }
    }

    private fun updateBattles(
        player: PlayerEntity,
        newBattles: List<Battle>,
    ) {
        val battleEntities =
            newBattles.map { battle ->
                BattleEntity(
                    battleKey = battle.battleKey,
                    battleAt = battle.battleAt,
                    event =
                        battle.event?.let {
                            BattleEntityEvent(
                                brawlStarsId = it.brawlStarsId,
                                modeBrawlStarsName = it.modeBrawlStarsName,
                                modeBrawlStarsId = it.modeBrawlStarsId,
                                map = it.map,
                            )
                        },
                    mode = battle.mode,
                    type = battle.type,
                    result = battle.result,
                    duration = battle.duration,
                    starPlayerBrawlStarsTag = battle.starPlayerBrawlStarsTag,
                    player = player,
                    battleRank = battle.battleRank,
                    trophyChange = battle.trophyChange,
                )
            }
        battleJpaRepository.saveAll(battleEntities)

        val teamPlayerEntities =
            newBattles.zip(battleEntities).flatMap { (battle, battleEntity) ->
                battle.teams.flatMapIndexed { teamIdx, team ->
                    team.mapIndexed { playerIdx, tp ->
                        BattleTeamPlayerEntity(
                            battle = battleEntity,
                            teamIdx = teamIdx,
                            playerIdx = playerIdx,
                            brawlStarsTag = tp.brawlStarsTag,
                            name = tp.name,
                            brawler =
                                BattleTeamPlayerEntityBrawler(
                                    brawlStarsId = tp.brawler.brawlStarsId,
                                    name = tp.brawler.name,
                                    power = tp.brawler.power,
                                    trophies = tp.brawler.trophies,
                                    trophyChange = tp.brawler.trophyChange,
                                ),
                        )
                    }
                }
            }
        battleTeamPlayerJpaRepository.saveAll(teamPlayerEntities)
    }

    private fun findOrCreate(response: PlayerResponse): Player =
        playerRepository.findWithLockByBrawlStarsTag(response.tag)
            ?: Player.create(response)
}
