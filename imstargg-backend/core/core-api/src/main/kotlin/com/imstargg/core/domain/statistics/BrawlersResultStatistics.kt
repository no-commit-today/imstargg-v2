package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlersResultStatistics(
    val brawlerIds: List<BrawlStarsId>,
    val totalBattleCount: Long,
    val winRate: Double,
)
