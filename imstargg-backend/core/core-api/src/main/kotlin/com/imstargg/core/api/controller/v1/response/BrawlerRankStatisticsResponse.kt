package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlerRankStatistics

data class BrawlerRankStatisticsResponse(
    val brawler: BrawlerResponse?,
    val totalBattleCount: Long,
    val averageRank: Double,
) {
    companion object {
        fun of(statistics: BrawlerRankStatistics, brawler: BrawlerResponse?) =
            BrawlerRankStatisticsResponse(
                brawler = brawler,
                totalBattleCount = statistics.totalBattleCount,
                averageRank = statistics.averageRank,
            )
    }
}
