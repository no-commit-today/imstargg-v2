package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlersResultStatistics

data class BrawlersResultStatisticsResponse(
    val brawlers: List<BrawlerResponse?>,
    val totalBattleCount: Long,
    val winRate: Double,
) {
    companion object {
        fun of(statistics: BrawlersResultStatistics, brawlers: List<BrawlerResponse?>) =
            BrawlersResultStatisticsResponse(
                brawlers = brawlers,
                totalBattleCount = statistics.totalBattleCount,
                winRate = statistics.winRate,
            )
    }
}
