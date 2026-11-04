package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerEventResultCount
import com.imstargg.core.domain.statistics.BrawlerPairResultCount
import com.imstargg.core.domain.statistics.BrawlerPairResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.domain.statistics.StatsRate
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.core.infrastructure.jpa.toResultCount
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerEnemyBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerPairBattleResultStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleResultStatisticsRepository(
    private val battleResultJpaRepository: BrawlerBattleResultStatsJpaRepository,
    private val enemyBattleResultJpaRepository: BrawlerEnemyBattleResultStatsJpaRepository,
    private val pairBattleResultJpaRepository: BrawlerPairBattleResultStatsJpaRepository,
) {
    fun findBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultStatistics {
        val rows: List<BrawlerEventResultCount> =
            battleResultJpaRepository
                .findGroupedByEventAndBrawler(
                    eventBrawlStarsId = eventId?.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerEventResultCount(
                        eventId = BrawlStarsId(projection.eventBrawlStarsId),
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        resultCount =
                            ResultCount(
                                victoryCount = projection.victoryCount,
                                defeatCount = projection.defeatCount,
                                drawCount = projection.drawCount,
                            ),
                        starPlayerCount = projection.starPlayerCount,
                    )
                }
        return BrawlerResultStatistics.create(rows = rows)
    }

    fun findBrawlerPairResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerPairResultStatistics {
        val counts: List<BrawlerPairResultCount> =
            pairBattleResultJpaRepository
                .findAllByBrawler(
                    brawlerBrawlStarsId = brawlerId.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerPairResultCount(
                        brawler1Id = BrawlStarsId(projection.brawler1BrawlStarsId),
                        brawler2Id = BrawlStarsId(projection.brawler2BrawlStarsId),
                        resultCount =
                            ResultCount(
                                victoryCount = projection.victoryCount,
                                defeatCount = projection.defeatCount,
                                drawCount = projection.drawCount,
                            ),
                    )
                }
        val totalTeamCount =
            StatsRate.pairTeamCount(
                pairBattleResultJpaRepository.sumTotalCount(
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ),
            )
        return BrawlerPairResultStatistics.create(
            brawlerId = brawlerId,
            totalTeamCount = totalTeamCount,
            counts = counts,
        )
    }

    fun findBrawlerEnemyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerEnemyResultStatistics {
        val counts: List<BrawlerEnemyResultCount> =
            enemyBattleResultJpaRepository
                .findAllByBrawler(
                    brawlerBrawlStarsId = brawlerId.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerEnemyResultCount(
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        enemyBrawlerId = BrawlStarsId(projection.enemyBrawlerBrawlStarsId),
                        resultCount =
                            ResultCount(
                                victoryCount = projection.victoryCount,
                                defeatCount = projection.defeatCount,
                                drawCount = projection.drawCount,
                            ),
                    )
                }
        return BrawlerEnemyResultStatistics.create(counts = counts)
    }

    fun findBrawlerDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultDailyStatistics {
        val dailyCounts: List<BrawlerResultDailyCount> =
            battleResultJpaRepository
                .findAllByBrawlerBrawlStarsIdAndTierRangeAndBattleDateBetween(
                    brawlerBrawlStarsId = brawlerId.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).groupBy { it.battleDate }
                .map { (battleDate, grouped) ->
                    BrawlerResultDailyCount(
                        date = battleDate,
                        brawlerResultCount =
                            BrawlerResultCount(
                                brawlerId = brawlerId,
                                resultCount =
                                    grouped
                                        .map { it.resultCount.toResultCount() }
                                        .reduce(ResultCount::merge),
                                starPlayerCount = grouped.sumOf { it.starPlayerCount },
                            ),
                    )
                }.sortedBy { it.date }
        return BrawlerResultDailyStatistics.create(brawlerId = brawlerId, dailyCounts = dailyCounts)
    }
}
