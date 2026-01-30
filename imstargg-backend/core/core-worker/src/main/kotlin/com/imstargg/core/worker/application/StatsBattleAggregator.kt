package com.imstargg.core.worker.application

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.lock.LockName
import com.imstargg.infrastructure.db.core.lock.LockTemplate
import com.imstargg.infrastructure.db.core.statistics.StatsBattleHelper
import com.imstargg.infrastructure.db.core.statistics.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.StatsRankBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.StatsResultBattleJpaRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class StatsBattleAggregator(
    private val clock: Clock,
    private val lockTemplate: LockTemplate,
    private val statsRankBattleRepository: StatsRankBattleJpaRepository,
    private val statsResultBattleRepository: StatsResultBattleJpaRepository,
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
                    playerBattles.forEach { battle ->
                        aggregateBattle(battle)
                    }
                }
            }
    }

    private fun aggregateBattle(battle: BattleEntity) {
        if (StatsRankBattleEntity.canCreate(battle)) {
            statsRankBattleRepository.save(StatsRankBattleEntity.from(battle))
        }

        if (StatsResultBattleEntity.canCreate(battle)) {
            statsResultBattleRepository.save(StatsResultBattleEntity.from(battle))
        }
    }
}
