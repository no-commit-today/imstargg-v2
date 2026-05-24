package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerRankCount(
    val brawlerId: BrawlStarsId,
    val rankCount: RankCount,
) {
    fun toStatisticsItem() =
        BrawlerRankStatisticsItem(
            brawlerId = brawlerId,
            totalBattleCount = rankCount.totalCount,
            averageRank = rankCount.averageRank,
        )
}
