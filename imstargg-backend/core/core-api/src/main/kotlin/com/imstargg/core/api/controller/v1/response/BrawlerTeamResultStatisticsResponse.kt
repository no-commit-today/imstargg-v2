package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.api.controller.PageResponse
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.statistics.BrawlerTeamResultStatistics

data class BrawlerTeamResultStatisticsItemResponse(
    val brawlers: List<BrawlerResponse?>,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

data class BrawlerTeamResultStatisticsResponse(
    val totalBattleCount: Long,
    val items: PageResponse<BrawlerTeamResultStatisticsItemResponse>,
) {
    companion object {
        fun of(
            statistics: BrawlerTeamResultStatistics,
            brawlers: Brawlers,
        ) = BrawlerTeamResultStatisticsResponse(
            totalBattleCount = statistics.totalBattleCount,
            items =
                PageResponse.from(
                    statistics.items.map { item ->
                        BrawlerTeamResultStatisticsItemResponse(
                            brawlers =
                                item.brawlerIds.map { id ->
                                    brawlers.find(id)?.let { BrawlerResponse.of(it) }
                                },
                            totalBattleCount = item.totalBattleCount,
                            pickRate = item.pickRate,
                            winRate = item.winRate,
                        )
                    },
                ),
        )
    }
}
