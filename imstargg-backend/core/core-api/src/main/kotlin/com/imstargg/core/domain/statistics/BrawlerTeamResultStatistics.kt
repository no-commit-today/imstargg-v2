package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.Page
import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerTeamResultStatisticsItem(
    val brawlerIds: List<BrawlStarsId>,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

data class BrawlerTeamResultStatistics(
    val totalBattleCount: Long,
    val items: Page<BrawlerTeamResultStatisticsItem>,
) {
    companion object {
        fun create(
            totalBattleCount: Long,
            counts: Page<BrawlerTeamResultCount>,
        ): BrawlerTeamResultStatistics {
            val items =
                counts.map { count ->
                    BrawlerTeamResultStatisticsItem(
                        brawlerIds = count.brawlerIds,
                        totalBattleCount = count.resultCount.totalCount,
                        pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalBattleCount),
                        winRate = count.resultCount.winRate,
                    )
                }
            return BrawlerTeamResultStatistics(totalBattleCount = totalBattleCount, items = items)
        }
    }
}
