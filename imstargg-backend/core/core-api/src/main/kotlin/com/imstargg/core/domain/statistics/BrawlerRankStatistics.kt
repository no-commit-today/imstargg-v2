package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerRankStatisticsItem(
    val brawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val averageRank: Double,
)

data class BrawlerRankStatistics(
    val items: List<BrawlerRankStatisticsItem>,
) {
    companion object {
        fun create(counts: List<BrawlerRankCount>): BrawlerRankStatistics {
            val items =
                counts
                    .groupBy { it.brawlerId }
                    .map { (brawlerId, grouped) ->
                        BrawlerRankCount(
                            brawlerId = brawlerId,
                            rankCount = grouped.map { it.rankCount }.reduce(RankCount::merge),
                        ).toStatisticsItem()
                    }
            return BrawlerRankStatistics(items = items)
        }
    }
}
