package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import java.time.LocalDate

data class BrawlerResultDailyCount(
    val date: LocalDate,
    val resultCount: ResultCount,
    val starPlayerCount: StarPlayerCount,
) {
    fun toStatistics(brawlerId: BrawlStarsId) =
        BrawlerResultDailyStatistics(
            date = date,
            brawlerId = brawlerId,
            totalBattleCount = resultCount.totalCount,
            winRate = resultCount.winRate,
            starPlayerRate = starPlayerCount.starPlayerRate(resultCount.totalCount),
        )
}
