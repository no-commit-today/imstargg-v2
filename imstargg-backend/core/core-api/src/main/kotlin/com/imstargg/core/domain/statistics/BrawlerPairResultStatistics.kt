package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerPairResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    /** 조합에서 [brawlerId] 가 아닌 쪽. */
    val pairBrawlerId: BrawlStarsId,
    /** 이 조합이 성립한 팀 수(= 조합 행의 `total_count` 합). */
    val battleCount: Long,
    val pickRate: Double,
    /** 함께일 때 승률. */
    val winRate: Double,
    val tierScore: TierScore,
)

/**
 * 결과형 Pair(아군 조합) 통계 — 브롤러 X 가 낀 2인 조합 목록.
 * 분모·battleCount 의미와 정정 이력은 [StatsRate.pickRate] 가 정본, 하한·상위 N 은 [ComboStatsPolicy].
 * Enemy 통계와 구조가 닮았지만 의미가 다른 별개 모델 — 통합하지 말 것 (중복 유지 + 테스트 고정).
 */
data class BrawlerPairResultStatistics(
    val items: List<BrawlerPairResultStatisticsItem>,
) {
    companion object {
        /**
         * @param brawlerId 기준 브롤러 X. [counts] 의 각 조합은 X 를 brawler1 이나 brawler2 로 가진다.
         * @param totalTeamCount 같은 (티어, 기간)의 팀 수 — [StatsRate.pairTeamCount] 로 구한다.
         * @param counts X 가 낀 조합별 기간 합산 결과
         */
        fun create(
            brawlerId: BrawlStarsId,
            totalTeamCount: Long,
            counts: List<BrawlerPairResultCount>,
        ): BrawlerPairResultStatistics {
            // 수축의 기준(평균 승률)이 X 의 모든 조합을 보도록 하한을 적용하기 전에 계산한다.
            val tierScores =
                TierScore.create(
                    counts.associate { (it.brawler1Id to it.brawler2Id) to it.resultCount },
                    pickRateWeight = false,
                )
            val items =
                counts
                    .filter { it.resultCount.totalCount >= ComboStatsPolicy.BRAWLER_MIN_BATTLE_COUNT }
                    .map { count ->
                        BrawlerPairResultStatisticsItem(
                            brawlerId = brawlerId,
                            pairBrawlerId = if (count.brawler1Id == brawlerId) count.brawler2Id else count.brawler1Id,
                            battleCount = count.resultCount.totalCount,
                            pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalTeamCount),
                            winRate = count.resultCount.winRate,
                            tierScore = tierScores[count.brawler1Id to count.brawler2Id] ?: TierScore(50),
                        )
                    }.sortedByDescending { it.tierScore.value }
                    .take(ComboStatsPolicy.BRAWLER_TOP_N)
            return BrawlerPairResultStatistics(items = items)
        }
    }
}
