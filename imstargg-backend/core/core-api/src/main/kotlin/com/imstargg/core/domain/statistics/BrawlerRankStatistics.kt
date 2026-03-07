package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerRankStatistics(
    val brawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val averageRank: Double,
)
