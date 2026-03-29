package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlersResultStatisticsItem(
    val brawlerIds: List<BrawlStarsId>,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

data class BrawlersResultStatistics(
    val totalBattleCount: Long,
    val items: List<BrawlersResultStatisticsItem>,
) {
    companion object {
        fun create(counts: List<BrawlersResultCount>): BrawlersResultStatistics {
            val totalBattleCount = counts.sumOf { it.resultCount.totalCount }
            val items =
                counts.map { count ->
                    BrawlersResultStatisticsItem(
                        brawlerIds = count.brawlerIds,
                        totalBattleCount = count.resultCount.totalCount,
                        pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalBattleCount),
                        winRate = count.resultCount.winRate,
                    )
                }
            return BrawlersResultStatistics(totalBattleCount = totalBattleCount, items = items)
        }
    }
}
