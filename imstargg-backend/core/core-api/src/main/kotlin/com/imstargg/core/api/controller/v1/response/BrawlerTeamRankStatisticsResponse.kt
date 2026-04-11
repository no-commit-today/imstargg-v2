package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlerTeamRankStatisticsItem

data class BrawlerTeamRankStatisticsResponse(
    val brawlers: List<BrawlerResponse?>,
    val totalBattleCount: Long,
    val averageRank: Double,
) {
    companion object {
        fun of(
            statistics: BrawlerTeamRankStatisticsItem,
            brawlers: List<BrawlerResponse?>,
        ) = BrawlerTeamRankStatisticsResponse(
            brawlers = brawlers,
            totalBattleCount = statistics.totalBattleCount,
            averageRank = statistics.averageRank,
        )
    }
}
