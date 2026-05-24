package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlerRankStatisticsItem

data class BrawlerRankStatisticsResponse(
    val brawler: BrawlerResponse?,
    val totalBattleCount: Long,
    val averageRank: Double,
) {
    companion object {
        fun of(
            item: BrawlerRankStatisticsItem,
            brawler: BrawlerResponse?,
        ) = BrawlerRankStatisticsResponse(
            brawler = brawler,
            totalBattleCount = item.totalBattleCount,
            averageRank = item.averageRank,
        )
    }
}
