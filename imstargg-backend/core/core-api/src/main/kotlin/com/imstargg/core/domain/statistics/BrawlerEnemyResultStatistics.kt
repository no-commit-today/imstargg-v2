package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerEnemyResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    val enemyBrawlerId: BrawlStarsId,
    /** 해당 매치업이 등장한 슬롯 수 (= 전투 기록 수). 전체 표본은 wrapper 의 `totalBattleCount` 참조. */
    val battleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)

/**
 * 결과형 Enemy(상대) 통계 — winRate 는 "상대로 만났을 때 승률".
 * 분모·battleCount 의미와 정정 이력은 [StatsRate.pickRate] 가 정본.
 * Pair 통계와 구조가 닮았지만 의미가 다른 별개 모델 — 통합하지 말 것 (중복 유지 + 테스트 고정).
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
                        battleCount = count.resultCount.totalCount,
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
