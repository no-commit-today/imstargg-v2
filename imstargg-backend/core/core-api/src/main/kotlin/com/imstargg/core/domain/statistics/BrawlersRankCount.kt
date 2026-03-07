package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlersRankCount(
    val brawlerIds: List<BrawlStarsId>,
    val rankCount: RankCount,
) {
    fun toStatistics() =
        BrawlersRankStatistics(
            brawlerIds = brawlerIds,
            totalBattleCount = rankCount.totalCount,
            averageRank = rankCount.averageRank,
        )
}
