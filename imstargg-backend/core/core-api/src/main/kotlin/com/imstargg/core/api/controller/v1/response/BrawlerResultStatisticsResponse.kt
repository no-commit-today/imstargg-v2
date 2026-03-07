package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlerResultStatistics

data class BrawlerResultStatisticsResponse(
    val brawlerId: Long,
    val totalBattleCount: Long,
    val winRate: Double,
    val starPlayerRate: Double,
) {
    companion object {
        fun of(statistics: BrawlerResultStatistics) =
            BrawlerResultStatisticsResponse(
                brawlerId = statistics.brawlerId.value,
                totalBattleCount = statistics.totalBattleCount,
                winRate = statistics.winRate,
                starPlayerRate = statistics.starPlayerRate,
            )
    }
}
