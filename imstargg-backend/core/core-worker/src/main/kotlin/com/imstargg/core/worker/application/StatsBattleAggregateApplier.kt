package com.imstargg.core.worker.application

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.statistics.AggregatedRankBattleKeyEntity
import com.imstargg.infrastructure.db.core.statistics.AggregatedRankBattleKeyJpaRepository
import com.imstargg.infrastructure.db.core.statistics.AggregatedResultBattleKeyEntity
import com.imstargg.infrastructure.db.core.statistics.AggregatedResultBattleKeyJpaRepository
import com.imstargg.infrastructure.db.core.statistics.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.StatsRankBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.StatsResultBattleJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class StatsBattleAggregateApplier(
    private val statsRankBattleRepository: StatsRankBattleJpaRepository,
    private val statsResultBattleRepository: StatsResultBattleJpaRepository,
    private val aggregatedRankBattleKeyRepository: AggregatedRankBattleKeyJpaRepository,
    private val aggregatedResultBattleKeyRepository: AggregatedResultBattleKeyJpaRepository,
) {
    @Transactional
    fun apply(playerBattles: List<BattleEntity>) {
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
