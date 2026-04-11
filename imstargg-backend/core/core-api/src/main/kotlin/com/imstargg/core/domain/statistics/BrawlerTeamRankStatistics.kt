package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerTeamRankStatisticsItem(
    val brawlerIds: List<BrawlStarsId>,
    val totalBattleCount: Long,
    val averageRank: Double,
)

data class BrawlerTeamRankStatistics(
    val totalBattleCount: Long,
    val totalElementCount: Long,
    val items: List<BrawlerTeamRankStatisticsItem>,
)
