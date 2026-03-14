package com.imstargg.core.worker.application

import com.imstargg.core.worker.domain.Battle
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityEvent
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntityBrawler
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BattleUpdateApplier(
    private val battleRepository: BattleJpaRepository,
    private val battleTeamPlayerRepository: BattleTeamPlayerJpaRepository,
) {
    @Transactional
    fun apply(
        player: PlayerEntity,
        battles: List<Battle>,
    ): List<Battle> {
        val latestBattle = battleRepository.findTopByPlayerIdOrderByBattleAtDesc(player.id!!)
        val latestBattleTime = latestBattle?.battleAt

        val newBattles =
            if (latestBattleTime != null) {
                battles.filter { it.battleAt.isAfter(latestBattleTime) }
            } else {
                battles
            }

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
        battleRepository.saveAll(battleEntities)

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
        battleTeamPlayerRepository.saveAll(teamPlayerEntities)

        return newBattles
    }
}
