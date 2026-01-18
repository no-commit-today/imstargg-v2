package com.imstargg.core.worker.application

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import org.springframework.stereotype.Component

@Component
class BattleUpdateApplier(
    private val battleRepository: BattleJpaRepository,
) {
    fun apply(
        player: PlayerEntity,
        battles: List<BattleEntity>,
    ): List<BattleEntity> {
        val latestBattle = battleRepository.findTopByPlayerIdOrderByBattleAtDesc(player.id!!)
        val latestBattleTime = latestBattle?.battleAt

        val newBattles =
            if (latestBattleTime != null) {
                battles.filter { it.battleAt.isAfter(latestBattleTime) }
            } else {
                battles
            }

        return battleRepository.saveAll(newBattles)
    }
}
