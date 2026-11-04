package com.imstargg.core.domain.statistics

/**
 * 전체 3인 조합 정렬 — 조회 조건(티어, 기간)의 3인 조합을 티어 스코어 순으로 줄 세운 상위 목록.
 * 분모·battleCount 의미는 [StatsRate.pickRate] 가 정본, 하한·상위 N 은 [ComboStatsPolicy].
 * [PairResultRanking] 과 규칙이 같고, 분모만 3인 조합 행 합 그대로다(÷3 하지 않는다).
 *
 * 정렬·상위 N 은 DB 가 아니라 도메인에서 한다 — 티어 스코어가 기간 전체 평균 승률을 필요로 한다(스펙 §3.5).
 */
data class TrioResultRanking(
    val items: List<ComboResultRankingItem>,
) {
    companion object {
        /**
         * @param totalTeamCount 같은 (티어, 기간)의 팀 수 — 하한을 적용하기 전 모든 3인 조합 행의 `total_count` 합
         *   그대로다. 하한 미만 조합도 분모에는 들어간다.
         * @param counts 조합별 기간 합산 결과. 비어 있으면 빈 목록을 돌려준다.
         */
        fun create(
            totalTeamCount: Long,
            counts: List<BrawlerTrioResultCount>,
        ): TrioResultRanking {
            val tierScores =
                TierScore.create(
                    counts.associate { Triple(it.brawler1Id, it.brawler2Id, it.brawler3Id) to it.resultCount },
                    pickRateWeight = false,
                )
            val items =
                counts
                    .filter { it.resultCount.totalCount >= ComboStatsPolicy.RANKING_MIN_BATTLE_COUNT }
                    .map { count ->
                        ComboResultRankingItem(
                            brawlerIds = listOf(count.brawler1Id, count.brawler2Id, count.brawler3Id),
                            battleCount = count.resultCount.totalCount,
                            pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalTeamCount),
                            winRate = count.resultCount.winRate,
                            tierScore =
                                tierScores[Triple(count.brawler1Id, count.brawler2Id, count.brawler3Id)]
                                    ?: TierScore(50),
                        )
                    }.sortedByDescending { it.tierScore.value }
                    .take(ComboStatsPolicy.RANKING_TOP_N)
            return TrioResultRanking(items = items)
        }
    }
}
