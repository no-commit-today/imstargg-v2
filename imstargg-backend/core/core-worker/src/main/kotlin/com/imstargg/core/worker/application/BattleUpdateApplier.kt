package com.imstargg.core.worker.application

import com.imstargg.core.worker.domain.BattleResult
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleJpaRepository
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
        buildResults: List<BattleResult>,
    ): List<BattleResult> {
        val latestBattle = battleRepository.findTopByPlayerIdOrderByBattleAtDesc(player.id!!)
        val latestBattleTime = latestBattle?.battleAt

        val newResults =
            if (latestBattleTime != null) {
                buildResults.filter { it.battle.battleAt.isAfter(latestBattleTime) }
            } else {
                buildResults
            }

        battleRepository.saveAll(newResults.map { it.battle })
        battleTeamPlayerRepository.saveAll(newResults.flatMap { it.teamPlayers })

        return newResults
    }
}
