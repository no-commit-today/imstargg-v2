package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerEnemyResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    val enemyBrawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

data class BrawlerEnemyResultStatistics(
    val totalBattleCount: Long,
    val totalElementCount: Long,
    val items: List<BrawlerEnemyResultStatisticsItem>,
) {
    companion object {
        fun create(
            totalBattleCount: Long,
            totalElementCount: Long,
            counts: List<BrawlerEnemyResultCount>,
        ): BrawlerEnemyResultStatistics {
            val items =
                counts.map { count ->
                    BrawlerEnemyResultStatisticsItem(
                        brawlerId = count.brawlerId,
                        enemyBrawlerId = count.enemyBrawlerId,
                        totalBattleCount = count.resultCount.totalCount,
                        pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalBattleCount),
                        winRate = count.resultCount.winRate,
                    )
                }
            return BrawlerEnemyResultStatistics(
                totalBattleCount = totalBattleCount,
                totalElementCount = totalElementCount,
                items = items,
            )
        }
    }
}
