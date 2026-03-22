package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlersRankStatistics

data class BrawlersRankStatisticsResponse(
    val brawlers: List<BrawlerResponse?>,
    val totalBattleCount: Long,
    val averageRank: Double,
) {
    companion object {
        fun of(statistics: BrawlersRankStatistics, brawlers: List<BrawlerResponse?>) =
            BrawlersRankStatisticsResponse(
                brawlers = brawlers,
                totalBattleCount = statistics.totalBattleCount,
                averageRank = statistics.averageRank,
            )
    }
}
