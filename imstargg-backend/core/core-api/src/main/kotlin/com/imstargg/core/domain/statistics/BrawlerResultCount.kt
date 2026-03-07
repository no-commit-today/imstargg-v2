package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerResultCount(
    val brawlerId: BrawlStarsId,
    val resultCount: ResultCount,
    val starPlayerCount: StarPlayerCount,
) {
    fun toStatistics() =
        BrawlerResultStatistics(
            brawlerId = brawlerId,
            totalBattleCount = resultCount.totalCount,
            winRate = resultCount.winRate,
            starPlayerRate = starPlayerCount.starPlayerRate(resultCount.totalCount),
        )
}
