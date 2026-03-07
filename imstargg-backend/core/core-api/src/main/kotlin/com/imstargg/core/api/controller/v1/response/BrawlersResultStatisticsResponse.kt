package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlersResultStatistics

data class BrawlersResultStatisticsResponse(
    val brawlerIds: List<Long>,
    val totalBattleCount: Long,
    val winRate: Double,
) {
    companion object {
        fun of(statistics: BrawlersResultStatistics) =
            BrawlersResultStatisticsResponse(
                brawlerIds = statistics.brawlerIds.map { it.value },
                totalBattleCount = statistics.totalBattleCount,
                winRate = statistics.winRate,
            )
    }
}
