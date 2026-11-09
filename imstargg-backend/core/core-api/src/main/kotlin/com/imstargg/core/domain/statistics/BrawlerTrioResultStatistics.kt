package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerTrioResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    /** 조합에서 [brawlerId] 가 아닌 두 브롤러(id 오름차순). */
    val pairBrawlerIds: List<BrawlStarsId>,
    /** 이 조합이 성립한 팀 수(= 조합 행의 `total_count` 합). */
    val battleCount: Long,
    val pickRate: Double,
    /** 함께일 때 승률. */
    val winRate: Double,
    val tierScore: TierScore,
)

/**
 * 결과형 Trio(아군 3인 조합) 통계 — 브롤러 X 가 낀 3인 조합 목록.
 * 분모·battleCount 의미와 정정 이력은 [StatsRate.pickRate] 가 정본, 하한·상위 N 은 [ComboStatsPolicy].
 * [BrawlerPairResultStatistics] 와 규칙이 같은 평행 사본이다 — 통합하지 말 것 (중복 유지 + 테스트 고정).
 * 다른 점은 분모다. 3인 조합 행 하나가 팀 하나라 조합 행 합이 곧 팀 수다(÷3 하지 않는다).
 */
data class BrawlerTrioResultStatistics(
    val items: List<BrawlerTrioResultStatisticsItem>,
) {
    companion object {
        /**
         * @param brawlerId 기준 브롤러 X. [counts] 의 각 조합은 X 를 brawler1·brawler2·brawler3 중 하나로 가진다.
         * @param totalTeamCount 같은 (티어, 기간)의 팀 수 — 3인 조합 행의 `total_count` 합 그대로다.
         * @param counts X 가 낀 조합별 기간 합산 결과
         */
        fun create(
            brawlerId: BrawlStarsId,
            totalTeamCount: Long,
            counts: List<BrawlerTrioResultCount>,
        ): BrawlerTrioResultStatistics {
            // 수축 기준은 X 자기 평균이 아니라 전역 기준 0.5 다(ComboStatsPolicy.COMBO_BASELINE_WIN_RATE).
            val tierScores =
                TierScore.create(
                    counts.associate { Triple(it.brawler1Id, it.brawler2Id, it.brawler3Id) to it.resultCount },
                    baselineWinRate = ComboStatsPolicy.COMBO_BASELINE_WIN_RATE,
                )
            val items =
                counts
                    .filter { it.resultCount.totalCount >= ComboStatsPolicy.BRAWLER_MIN_BATTLE_COUNT }
                    .map { count ->
                        BrawlerTrioResultStatisticsItem(
                            brawlerId = brawlerId,
                            pairBrawlerIds =
                                listOf(count.brawler1Id, count.brawler2Id, count.brawler3Id)
                                    .filter { it != brawlerId }
                                    .sortedBy { it.value },
                            battleCount = count.resultCount.totalCount,
                            pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalTeamCount),
                            winRate = count.resultCount.winRate,
                            tierScore =
                                tierScores[Triple(count.brawler1Id, count.brawler2Id, count.brawler3Id)]
                                    ?: TierScore(50),
                        )
                    }.sortedByDescending { it.tierScore.value }
                    .take(ComboStatsPolicy.BRAWLER_TOP_N)
            return BrawlerTrioResultStatistics(items = items)
        }
    }
}
