package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerEnemyResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    val enemyBrawlerId: BrawlStarsId,
    /** X 가 이 상대를 만난 횟수(= X 기준 카운터 행의 `total_count` 합). */
    val battleCount: Long,
    val pickRate: Double,
    /** winRate 는 X 가 Y 를 만났을 때 X 의 승률. */
    val winRate: Double,
)

/**
 * 결과형 Enemy(상대) 통계 — 브롤러 X 가 만난 상대 Y 목록.
 * 분모·battleCount 의미와 정정 이력은 [StatsRate.pickRate] 가 정본, 하한·상위 N 은 [ComboStatsPolicy].
 * Pair 통계와 구조가 닮았지만 의미가 다른 별개 모델 — 통합하지 말 것 (중복 유지 + 테스트 고정).
 */
data class BrawlerEnemyResultStatistics(
    val items: List<BrawlerEnemyResultStatisticsItem>,
) {
    companion object {
        /**
         * @param counts X 기준 카운터 행(상대별 기간 합산). 분모(X 의 게임 수)는 하한을 적용하기 전 전체 행에서 구한다.
         */
        fun create(counts: List<BrawlerEnemyResultCount>): BrawlerEnemyResultStatistics {
            val totalGameCount = StatsRate.enemyGameCount(counts.sumOf { it.resultCount.totalCount })
            val items =
                counts
                    .filter { it.resultCount.totalCount >= ComboStatsPolicy.BRAWLER_MIN_BATTLE_COUNT }
                    .map { count ->
                        BrawlerEnemyResultStatisticsItem(
                            brawlerId = count.brawlerId,
                            enemyBrawlerId = count.enemyBrawlerId,
                            battleCount = count.resultCount.totalCount,
                            pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalGameCount),
                            winRate = count.resultCount.winRate,
                        )
                    }.sortedBy { it.winRate }
                    .take(ComboStatsPolicy.BRAWLER_TOP_N)
            return BrawlerEnemyResultStatistics(items = items)
        }
    }
}
