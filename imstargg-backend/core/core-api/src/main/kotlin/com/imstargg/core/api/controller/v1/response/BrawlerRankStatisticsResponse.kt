package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlerRankStatistics

data class BrawlerRankStatisticsResponse(
    val brawlerId: Long,
    val totalBattleCount: Long,
    val averageRank: Double,
) {
    companion object {
        fun of(statistics: BrawlerRankStatistics) =
            BrawlerRankStatisticsResponse(
                brawlerId = statistics.brawlerId.value,
                totalBattleCount = statistics.totalBattleCount,
                averageRank = statistics.averageRank,
            )
    }
}
