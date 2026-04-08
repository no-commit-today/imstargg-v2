package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerEnemyBattleResultStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleEventResultStatisticsRepository(
    private val battleResultJpaRepository: BrawlerBattleResultStatsJpaRepository,
    private val enemyBattleResultJpaRepository: BrawlerEnemyBattleResultStatsJpaRepository,
) {
    fun findBrawlerResultCounts(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultCount> {
        val entities =
            battleResultJpaRepository
                .findAllByEventBrawlStarsIdAndTierRangeAndBattleDateBetween(eventId.value, tierRange, startDate, endDate)

        return entities
            .groupBy { it.brawlerBrawlStarsId }
            .map { (brawlerBrawlStarsId, grouped) ->
                BrawlerResultCount(
                    brawlerId = BrawlStarsId(brawlerBrawlStarsId),
                    resultCount =
                        ResultCount(
                            grouped.sumOf { it.resultCount.victoryCount },
                            grouped.sumOf { it.resultCount.defeatCount },
                            grouped.sumOf { it.resultCount.drawCount },
                        ),
                    starPlayerCount = grouped.sumOf { it.starPlayerCount },
                )
            }
    }

    fun findBrawlerEnemyResultCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlerEnemyResultCount> {
        val entities =
            enemyBattleResultJpaRepository
                .findAllByEventBrawlStarsIdAndBrawlerBrawlStarsIdAndPeriodTypeAndTierRangeAndStartBattleDate(
                    eventId.value,
                    brawlerId.value,
                    StatsPeriodType.WEEKLY,
                    tierRange,
                    startDate,
                )

        return entities.map {
            BrawlerEnemyResultCount(
                brawlerId = BrawlStarsId(it.brawlerBrawlStarsId),
                enemyBrawlerId = BrawlStarsId(it.enemyBrawlerBrawlStarsId),
                resultCount = ResultCount(it.resultCount.victoryCount, it.resultCount.defeatCount, it.resultCount.drawCount),
            )
        }
    }
}
