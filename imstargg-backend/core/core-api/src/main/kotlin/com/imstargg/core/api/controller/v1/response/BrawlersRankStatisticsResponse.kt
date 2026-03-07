package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlersRankStatistics

data class BrawlersRankStatisticsResponse(
    val brawlerIds: List<Long>,
    val totalBattleCount: Long,
    val averageRank: Double,
) {
    companion object {
        fun of(statistics: BrawlersRankStatistics) =
            BrawlersRankStatisticsResponse(
                brawlerIds = statistics.brawlerIds.map { it.value },
                totalBattleCount = statistics.totalBattleCount,
                averageRank = statistics.averageRank,
            )
    }
}
