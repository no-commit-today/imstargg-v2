package com.imstargg.core.domain.statistics

import java.time.LocalDate

data class BrawlerResultDailyCount(
    val date: LocalDate,
    val brawlerResultCount: BrawlerResultCount,
) {
    fun toStatistics() =
        BrawlerResultDailyStatistics(
            date = date,
            brawlerId = brawlerResultCount.brawlerId,
            totalBattleCount = brawlerResultCount.resultCount.totalCount,
            winRate = brawlerResultCount.resultCount.winRate,
            starPlayerRate = brawlerResultCount.starPlayerCount
                .starPlayerRate(brawlerResultCount.resultCount.totalCount),
        )
}
