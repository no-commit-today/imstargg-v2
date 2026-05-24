package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerRankStatisticsItem(
    val brawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val averageRank: Double,
)

data class BrawlerRankStatistics(
    val items: List<BrawlerRankStatisticsItem>,
)
