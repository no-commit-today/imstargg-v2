package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerPairResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    val pairBrawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

data class BrawlerPairResultStatistics(
    val totalBattleCount: Long,
    val totalElementCount: Long,
    val items: List<BrawlerPairResultStatisticsItem>,
) {
    companion object {
        fun create(
            totalBattleCount: Long,
            totalElementCount: Long,
            counts: List<BrawlerPairResultCount>,
        ): BrawlerPairResultStatistics {
            val items =
                counts.map { count ->
                    BrawlerPairResultStatisticsItem(
                        brawlerId = count.brawlerId,
                        pairBrawlerId = count.pairBrawlerId,
                        totalBattleCount = count.resultCount.totalCount,
                        pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalBattleCount),
                        winRate = count.resultCount.winRate,
                    )
                }
            return BrawlerPairResultStatistics(
                totalBattleCount = totalBattleCount,
                totalElementCount = totalElementCount,
                items = items,
            )
        }
    }
}
