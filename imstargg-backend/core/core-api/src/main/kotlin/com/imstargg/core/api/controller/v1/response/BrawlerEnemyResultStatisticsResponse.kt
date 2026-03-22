package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics

data class BrawlerEnemyResultStatisticsResponse(
    val brawler: BrawlerResponse?,
    val enemyBrawler: BrawlerResponse?,
    val totalBattleCount: Long,
    val winRate: Double,
) {
    companion object {
        fun of(
            statistics: BrawlerEnemyResultStatistics,
            brawler: BrawlerResponse?,
            enemyBrawler: BrawlerResponse?,
        ) = BrawlerEnemyResultStatisticsResponse(
            brawler = brawler,
            enemyBrawler = enemyBrawler,
            totalBattleCount = statistics.totalBattleCount,
            winRate = statistics.winRate,
        )
    }
}
