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
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PlayerUpdater(
    private val playerRepository: PlayerRepository,
    private val battleJpaRepository: BattleJpaRepository,
    private val battleTeamPlayerJpaRepository: BattleTeamPlayerJpaRepository,
    private val playerBrawlerStatsAccumulator: PlayerBrawlerStatsAccumulator,
) {
    @Transactional
    fun update(
        playerResponse: PlayerResponse,
        battles: List<Battle>,
    ): List<Battle> {
        val player = findOrCreate(playerResponse)
        player.update(playerResponse)
        playerRepository.save(player)
        val newBattles = filterNewBattles(player.entity, battles)
        updateBattles(player.entity, newBattles)
        playerBrawlerStatsAccumulator.accumulate(player.entity, newBattles)
        return newBattles
    }

    @Transactional
    fun updatePlayer(response: PlayerResponse) {
        val player = findOrCreate(response)
        player.update(response)
        playerRepository.save(player)
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
