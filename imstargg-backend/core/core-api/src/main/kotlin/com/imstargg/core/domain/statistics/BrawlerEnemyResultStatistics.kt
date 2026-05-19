package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerEnemyResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    val enemyBrawlerId: BrawlStarsId,
    /** 조회 범위의 게임 수 (분모와 동일). */
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

/**
 * 결과형 Enemy 통계.
 *
 * `totalBattleCount` 의 의미: 조회 범위의 **게임 수** (2026-05-19 정정).
 * 픽률 = enemy 매치업 슬롯 수 / 게임 수.
 */
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
                        totalBattleCount = totalBattleCount,
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
