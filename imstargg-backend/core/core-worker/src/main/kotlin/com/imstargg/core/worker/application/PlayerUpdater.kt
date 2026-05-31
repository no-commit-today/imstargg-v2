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

    private fun filterNewBattles(
        player: PlayerEntity,
        battles: List<Battle>,
    ): List<Battle> {
        val latestBattleTime =
            battleJpaRepository
                .findTopByPlayerIdOrderByBattleAtDesc(player.id!!)
                ?.battleAt
                ?: return battles
        return battles.filter { it.battleAt.isAfter(latestBattleTime) }
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
