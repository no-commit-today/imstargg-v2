package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerPairRankCount(
    val brawlerId: BrawlStarsId,
    val pairBrawlerId: BrawlStarsId,
    val totalCount: Long,
    val weightedRankSum: Long,
) {
    val averageRank: Double
        get() = if (totalCount > 0) weightedRankSum.toDouble() / totalCount else 0.0
}
