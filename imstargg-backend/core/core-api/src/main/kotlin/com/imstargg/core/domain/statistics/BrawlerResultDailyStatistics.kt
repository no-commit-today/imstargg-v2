package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import java.time.LocalDate

data class BrawlerResultDailyStatistics(
    val date: LocalDate,
    val brawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val winRate: Double,
    val starPlayerRate: Double,
) {
    companion object {
        fun create(dailyCount: BrawlerResultDailyCount): BrawlerResultDailyStatistics {
            val resultCount = dailyCount.brawlerResultCount.resultCount
            return BrawlerResultDailyStatistics(
                date = dailyCount.date,
                brawlerId = dailyCount.brawlerResultCount.brawlerId,
                totalBattleCount = resultCount.totalCount,
                winRate = resultCount.winRate,
                starPlayerRate =
                    StatsRate.starPlayerRate(
                        dailyCount.brawlerResultCount.starPlayerCount,
                        resultCount.totalCount,
                    ),
            )
        }
    }
}
