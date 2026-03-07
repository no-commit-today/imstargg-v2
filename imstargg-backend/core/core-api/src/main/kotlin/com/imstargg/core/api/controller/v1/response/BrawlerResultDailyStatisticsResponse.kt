package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import java.time.LocalDate

data class BrawlerResultDailyStatisticsResponse(
    val date: LocalDate,
    val brawlerId: Long,
    val totalBattleCount: Long,
    val winRate: Double,
    val starPlayerRate: Double,
) {
    companion object {
        fun of(statistics: BrawlerResultDailyStatistics) =
            BrawlerResultDailyStatisticsResponse(
                date = statistics.date,
                brawlerId = statistics.brawlerId.value,
                totalBattleCount = statistics.totalBattleCount,
                winRate = statistics.winRate,
                starPlayerRate = statistics.starPlayerRate,
            )
    }
}
