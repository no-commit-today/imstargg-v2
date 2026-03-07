package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlersRankStatistics(
    val brawlerIds: List<BrawlStarsId>,
    val totalBattleCount: Long,
    val averageRank: Double,
)
