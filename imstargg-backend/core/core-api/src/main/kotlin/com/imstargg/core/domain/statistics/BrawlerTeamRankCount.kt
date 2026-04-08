package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerTeamRankCount(
    val brawlerIds: List<BrawlStarsId>,
    val rankCount: RankCount,
) {
    fun toStatistics() =
        BrawlerTeamRankStatistics(
            brawlerIds = brawlerIds,
            totalBattleCount = rankCount.totalCount,
            averageRank = rankCount.averageRank,
        )
}
