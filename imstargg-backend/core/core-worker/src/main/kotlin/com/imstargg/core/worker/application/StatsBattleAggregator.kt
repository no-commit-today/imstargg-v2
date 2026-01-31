package com.imstargg.core.worker.application

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.lock.LockName
import com.imstargg.infrastructure.db.core.lock.LockTemplate
import com.imstargg.infrastructure.db.core.statistics.AggregatedRankBattleKeyEntity
import com.imstargg.infrastructure.db.core.statistics.AggregatedRankBattleKeyJpaRepository
import com.imstargg.infrastructure.db.core.statistics.AggregatedResultBattleKeyEntity
import com.imstargg.infrastructure.db.core.statistics.AggregatedResultBattleKeyJpaRepository
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
    private val aggregatedRankBattleKeyRepository: AggregatedRankBattleKeyJpaRepository,
    private val aggregatedResultBattleKeyRepository: AggregatedResultBattleKeyJpaRepository,
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
                    aggregatePlayerBattles(playerBattles)
                }
            }
    }

    private fun aggregatePlayerBattles(playerBattles: List<BattleEntity>) {
        val statsRankBattles =
            playerBattles
                .filter { StatsRankBattleEntity.canCreate(it) }
                .map { StatsRankBattleEntity.from(it) }

        val statsResultBattles =
            playerBattles
                .filter { StatsResultBattleEntity.canCreate(it) }
                .map { StatsResultBattleEntity.from(it) }

        val aggregatedRankKeys =
            aggregatedRankBattleKeyRepository
                .findAllByStatsRankBattles(statsRankBattles)
        val aggregatedResultKeys =
            aggregatedResultBattleKeyRepository
                .findAllByStatsResultBattles(statsResultBattles)

        val newRankBattles =
            statsRankBattles.filter { battle ->
                aggregatedRankKeys.none { it.matches(battle) }
            }
        val newResultBattles =
            statsResultBattles.filter { battle ->
                aggregatedResultKeys.none { it.matches(battle) }
            }

        statsRankBattleRepository.saveAll(newRankBattles)
        statsResultBattleRepository.saveAll(newResultBattles)

        aggregatedRankBattleKeyRepository.saveAll(
            newRankBattles.map { AggregatedRankBattleKeyEntity.from(it) },
        )
        aggregatedResultBattleKeyRepository.saveAll(
            newResultBattles.map { AggregatedResultBattleKeyEntity.from(it) },
        )
    }
}
