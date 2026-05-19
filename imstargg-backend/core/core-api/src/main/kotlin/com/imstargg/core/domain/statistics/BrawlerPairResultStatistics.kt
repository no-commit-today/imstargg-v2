package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerPairResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    val pairBrawlerId: BrawlStarsId,
    /** 조회 범위의 게임 수 (분모와 동일). */
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

/**
 * 결과형 Pair 통계.
 *
 * `totalBattleCount` 의 의미: 조회 범위의 **게임 수** (2026-05-19 정정).
 * 픽률 = pair 슬롯 수 / 게임 수.
 */
data class BrawlerPairResultStatistics(
    val totalBattleCount: Long,
    val totalElementCount: Long,
    val items: List<BrawlerPairResultStatisticsItem>,
) {
    companion object {
        fun create(
            totalBattleCount: Long,
            totalElementCount: Long,
            counts: List<BrawlerPairResultCount>,
        ): BrawlerPairResultStatistics {
            val items =
                counts.map { count ->
                    BrawlerPairResultStatisticsItem(
                        brawlerId = count.brawlerId,
                        pairBrawlerId = count.pairBrawlerId,
                        totalBattleCount = totalBattleCount,
                        pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalBattleCount),
                        winRate = count.resultCount.winRate,
                    )
                }
            return BrawlerPairResultStatistics(
                totalBattleCount = totalBattleCount,
                totalElementCount = totalElementCount,
                items = items,
            )
        }
    }
}
