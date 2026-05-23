package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import java.time.LocalDate

data class BrawlerResultDailyStatisticsItem(
    val date: LocalDate,
    val totalBattleCount: Long,
    val winRate: Double,
    val starPlayerRate: Double,
)

data class BrawlerResultDailyStatistics(
    val brawlerId: BrawlStarsId,
    val items: List<BrawlerResultDailyStatisticsItem>,
) {
    companion object {
        fun create(
            brawlerId: BrawlStarsId,
            dailyCounts: List<BrawlerResultDailyCount>,
        ): BrawlerResultDailyStatistics {
            val items =
                dailyCounts.map { dailyCount ->
                    val resultCount = dailyCount.brawlerResultCount.resultCount
                    BrawlerResultDailyStatisticsItem(
                        date = dailyCount.date,
                        totalBattleCount = resultCount.totalCount,
                        winRate = resultCount.winRate,
                        starPlayerRate =
                            StatsRate.starPlayerRate(
                                dailyCount.brawlerResultCount.starPlayerCount,
                                resultCount.totalCount,
                            ),
                    )
                }
            return BrawlerResultDailyStatistics(brawlerId = brawlerId, items = items)
        }
    }
}
