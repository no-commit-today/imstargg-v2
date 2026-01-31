package com.imstargg.core.worker.application

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.lock.LockName
import com.imstargg.infrastructure.db.core.lock.LockTemplate
import com.imstargg.infrastructure.db.core.statistics.StatsBattleHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class StatsBattleAggregator(
    private val clock: Clock,
    private val lockTemplate: LockTemplate,
    private val statsBattleAggregateApplier: StatsBattleAggregateApplier,
) {
    companion object {
        private const val LOCK_TIMEOUT_SECONDS = 3
    }

    @Async
    fun asyncAggregate(battles: List<BattleEntity>) {
        doAggregate(battles)
    }

    fun aggregate(battles: List<BattleEntity>) {
        doAggregate(battles)
    }

    private fun doAggregate(battles: List<BattleEntity>) {
        battles
            .filter { StatsBattleHelper.canAggregate(clock, it) }
            .groupBy { it.player.brawlStarsTag }
            .forEach { (tag, playerBattles) ->
                lockTemplate.execute(
                    LockName.createPlayerStatsBattleLockName(tag),
                    LOCK_TIMEOUT_SECONDS,
                ) {
                    statsBattleAggregateApplier.apply(playerBattles)
                }
            }
    }
}
