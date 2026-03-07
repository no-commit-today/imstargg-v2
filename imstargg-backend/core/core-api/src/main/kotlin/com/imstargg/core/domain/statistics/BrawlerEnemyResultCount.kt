package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerEnemyResultCount(
    val brawlerId: BrawlStarsId,
    val enemyBrawlerId: BrawlStarsId,
    val resultCount: ResultCount,
) {
    fun toStatistics() =
        BrawlerEnemyResultStatistics(
            brawlerId = brawlerId,
            enemyBrawlerId = enemyBrawlerId,
            totalBattleCount = resultCount.totalCount,
            winRate = resultCount.winRate,
        )
}
