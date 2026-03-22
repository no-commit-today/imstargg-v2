package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlerResultStatistics

data class BrawlerResultStatisticsResponse(
    val brawler: BrawlerResponse?,
    val totalBattleCount: Long,
    val winRate: Double,
    val starPlayerRate: Double,
) {
    companion object {
        fun of(statistics: BrawlerResultStatistics, brawler: BrawlerResponse?) =
            BrawlerResultStatisticsResponse(
                brawler = brawler,
                totalBattleCount = statistics.totalBattleCount,
                winRate = statistics.winRate,
                starPlayerRate = statistics.starPlayerRate,
            )
    }
}
