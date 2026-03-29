package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.statistics.BrawlersResultStatistics

data class BrawlersResultStatisticsItemResponse(
    val brawlers: List<BrawlerResponse?>,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

data class BrawlersResultStatisticsResponse(
    val totalBattleCount: Long,
    val items: List<BrawlersResultStatisticsItemResponse>,
) {
    companion object {
        fun of(
            statistics: BrawlersResultStatistics,
            brawlers: Brawlers,
        ) = BrawlersResultStatisticsResponse(
            totalBattleCount = statistics.totalBattleCount,
            items =
                statistics.items.map { item ->
                    BrawlersResultStatisticsItemResponse(
                        brawlers =
                            item.brawlerIds.map { id ->
                                brawlers.find(id)?.let { BrawlerResponse.of(it) }
                            },
                        totalBattleCount = item.totalBattleCount,
                        pickRate = item.pickRate,
                        winRate = item.winRate,
                    )
                },
        )
    }
}
