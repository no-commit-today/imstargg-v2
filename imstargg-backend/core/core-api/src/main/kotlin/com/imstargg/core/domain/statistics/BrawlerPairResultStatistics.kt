package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerPairResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    val pairBrawlerId: BrawlStarsId,
    /** 해당 pair 가 등장한 슬롯 수 (= 전투 기록 수). 전체 표본은 wrapper 의 `totalBattleCount` 참조. */
    val battleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

/**
 * 결과형 Pair(아군 조합) 통계 — winRate 는 "함께일 때 승률".
 * 분모·battleCount 의미와 정정 이력은 [StatsRate.pickRate] 가 정본.
 * Enemy 통계와 구조가 닮았지만 의미가 다른 별개 모델 — 통합하지 말 것 (중복 유지 + 테스트 고정).
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
                        battleCount = count.resultCount.totalCount,
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
