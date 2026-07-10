package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerPairRankStatisticsItem(
    val brawlerId: BrawlStarsId,
    val pairBrawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val averageRank: Double,
)

data class BrawlerPairRankStatistics(
    val totalBattleCount: Long,
    val totalElementCount: Long,
    val items: List<BrawlerPairRankStatisticsItem>,
) {
    companion object {
        fun create(
            totalBattleCount: Long,
            totalElementCount: Long,
            counts: List<BrawlerPairRankCount>,
        ): BrawlerPairRankStatistics =
            BrawlerPairRankStatistics(
                totalBattleCount = totalBattleCount,
                totalElementCount = totalElementCount,
                items =
                    counts.map { count ->
                        BrawlerPairRankStatisticsItem(
                            brawlerId = count.brawlerId,
                            pairBrawlerId = count.pairBrawlerId,
                            totalBattleCount = count.totalCount,
                            averageRank = count.averageRank,
                        )
                    },
            )
    }
}
