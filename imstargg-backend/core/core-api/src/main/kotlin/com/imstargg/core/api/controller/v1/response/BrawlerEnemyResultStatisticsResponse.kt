package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics

data class BrawlerEnemyResultStatisticsResponse(
    val brawlerId: Long,
    val enemyBrawlerId: Long,
    val totalBattleCount: Long,
    val winRate: Double,
) {
    companion object {
        fun of(statistics: BrawlerEnemyResultStatistics) =
            BrawlerEnemyResultStatisticsResponse(
                brawlerId = statistics.brawlerId.value,
                enemyBrawlerId = statistics.enemyBrawlerId.value,
                totalBattleCount = statistics.totalBattleCount,
                winRate = statistics.winRate,
            )
    }
}
