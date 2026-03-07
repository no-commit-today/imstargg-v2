package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerResultStatistics(
    val brawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val winRate: Double,
    val starPlayerRate: Double,
)
