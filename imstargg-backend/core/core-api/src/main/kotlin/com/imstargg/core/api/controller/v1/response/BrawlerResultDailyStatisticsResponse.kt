package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlerResultDailyStatisticsItem
import java.time.LocalDate

data class BrawlerResultDailyStatisticsResponse(
    val date: LocalDate,
    val brawler: BrawlerResponse?,
    val totalBattleCount: Long,
    val winRate: Double,
    val starPlayerRate: Double,
) {
    companion object {
        fun of(
            item: BrawlerResultDailyStatisticsItem,
            brawler: BrawlerResponse?,
        ) = BrawlerResultDailyStatisticsResponse(
            date = item.date,
            brawler = brawler,
            totalBattleCount = item.totalBattleCount,
            winRate = item.winRate,
            starPlayerRate = item.starPlayerRate,
        )
    }
}
