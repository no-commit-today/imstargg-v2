package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.statistics.BrawlerResultStatistics

data class BrawlerResultStatisticsItemResponse(
    val brawler: BrawlerResponse?,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
    val starPlayerRate: Double,
    val tierScore: Int,
    val tier: String,
)

data class BrawlerResultStatisticsResponse(
    val totalBattleCount: Long,
    val items: List<BrawlerResultStatisticsItemResponse>,
) {
    companion object {
        fun of(
            statistics: BrawlerResultStatistics,
            brawlers: Brawlers,
        ) = BrawlerResultStatisticsResponse(
            totalBattleCount = statistics.totalBattleCount,
            items =
                statistics.items.map { item ->
                    BrawlerResultStatisticsItemResponse(
                        brawler = brawlers.find(item.brawlerId)?.let { BrawlerResponse.of(it) },
                        totalBattleCount = item.totalBattleCount,
                        pickRate = item.pickRate,
                        winRate = item.winRate,
                        starPlayerRate = item.starPlayerRate,
                        tierScore = item.tierScore.value,
                        tier = item.tierScore.tier().name,
                    )
                },
        )
    }
}
