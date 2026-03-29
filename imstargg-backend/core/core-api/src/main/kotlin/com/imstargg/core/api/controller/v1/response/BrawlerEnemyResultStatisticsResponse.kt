package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics

data class BrawlerEnemyResultStatisticsItemResponse(
    val brawler: BrawlerResponse?,
    val enemyBrawler: BrawlerResponse?,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

data class BrawlerEnemyResultStatisticsResponse(
    val totalBattleCount: Long,
    val items: List<BrawlerEnemyResultStatisticsItemResponse>,
) {
    companion object {
        fun of(
            statistics: BrawlerEnemyResultStatistics,
            brawlers: Brawlers,
        ) = BrawlerEnemyResultStatisticsResponse(
            totalBattleCount = statistics.totalBattleCount,
            items =
                statistics.items.map { item ->
                    BrawlerEnemyResultStatisticsItemResponse(
                        brawler = brawlers.find(item.brawlerId)?.let { BrawlerResponse.of(it) },
                        enemyBrawler = brawlers.find(item.enemyBrawlerId)?.let { BrawlerResponse.of(it) },
                        totalBattleCount = item.totalBattleCount,
                        pickRate = item.pickRate,
                        winRate = item.winRate,
                    )
                },
        )
    }
}
