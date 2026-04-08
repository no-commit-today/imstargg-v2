package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
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
class BrawlerResultStatisticsRepository(
    private val battleResultJpaRepository: BrawlerBattleResultStatsJpaRepository,
    private val enemyBattleResultJpaRepository: BrawlerEnemyBattleResultStatsJpaRepository,
) {
    fun findBrawlerResultCounts(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultCount> {
        val entities = battleResultJpaRepository.findAllByTierRangeAndBattleDateBetween(tierRange, startDate, endDate)

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

    fun findBrawlerDailyResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultDailyCount> {
        val entities =
            battleResultJpaRepository
                .findAllByBrawlerBrawlStarsIdAndTierRangeAndBattleDateBetween(
                    brawlerId.value,
                    tierRange,
                    startDate,
                    endDate,
                )

        return entities
            .groupBy { it.battleDate }
            .map { (battleDate, grouped) ->
                BrawlerResultDailyCount(
                    date = battleDate,
                    brawlerResultCount =
                        BrawlerResultCount(
                            brawlerId = brawlerId,
                            resultCount =
                                ResultCount(
                                    grouped.sumOf { it.resultCount.victoryCount },
                                    grouped.sumOf { it.resultCount.defeatCount },
                                    grouped.sumOf { it.resultCount.drawCount },
                                ),
                            starPlayerCount = grouped.sumOf { it.starPlayerCount },
                        ),
                )
            }.sortedBy { it.date }
    }

    fun findBrawlerEnemyResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlerEnemyResultCount> {
        val entities =
            enemyBattleResultJpaRepository
                .findAllByBrawlerBrawlStarsIdAndPeriodTypeAndTierRangeAndStartBattleDate(
                    brawlerId.value,
                    StatsPeriodType.WEEKLY,
                    tierRange,
                    startDate,
                )

        return entities
            .groupBy { it.enemyBrawlerBrawlStarsId }
            .map { (enemyBrawlerBrawlStarsId, grouped) ->
                BrawlerEnemyResultCount(
                    brawlerId = brawlerId,
                    enemyBrawlerId = BrawlStarsId(enemyBrawlerBrawlStarsId),
                    resultCount =
                        ResultCount(
                            grouped.sumOf { it.resultCount.victoryCount },
                            grouped.sumOf { it.resultCount.defeatCount },
                            grouped.sumOf { it.resultCount.drawCount },
                        ),
                )
            }
    }
}
