package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
    val starPlayerRate: Double,
)

data class BrawlerResultStatistics(
    val totalBattleCount: Long,
    val items: List<BrawlerResultStatisticsItem>,
) {
    companion object {
        fun create(counts: List<BrawlerResultCount>): BrawlerResultStatistics {
            val totalBattleCount = counts.sumOf { it.resultCount.totalCount }
            val items =
                counts.map { count ->
                    BrawlerResultStatisticsItem(
                        brawlerId = count.brawlerId,
                        totalBattleCount = count.resultCount.totalCount,
                        pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalBattleCount),
                        winRate = count.resultCount.winRate,
                        starPlayerRate =
                            StatsRate.starPlayerRate(
                                count.starPlayerCount,
                                count.resultCount.totalCount,
                            ),
                    )
                }
            return BrawlerResultStatistics(totalBattleCount = totalBattleCount, items = items)
        }
    }
}
