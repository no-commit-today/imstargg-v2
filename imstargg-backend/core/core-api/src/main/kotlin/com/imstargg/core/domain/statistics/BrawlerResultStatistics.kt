package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    /** 해당 브롤러가 등장한 슬롯 수 (= 전투 기록 수). 전체 표본은 wrapper 의 `totalBattleCount` 참조. */
    val battleCount: Long,
    val pickRate: Double,
    val winRate: Double,
    val starPlayerRate: Double,
    val tierScore: BrawlerTierScore,
)

/**
 * 결과형 통계 - 브롤러 단위.
 *
 * `totalBattleCount` 의 의미: 조회 범위의 **게임 수** (이전엔 슬롯 합이었음 — 2026-05-19 정정).
 * 픽률 = 해당 브롤러가 등장한 슬롯 수 / 게임 수 → 3v3 의 경우 픽률 합이 ≈ 6.0.
 *
 * item 단위는 `battleCount`(브롤러별 전투 수)로 구분한다 — 2026-05-19 정정 때 item 에도 전역 게임 수를
 * 복사하는 바람에 프론트 "전투수" 컬럼이 전 브롤러 동일 값으로 표시됐던 버그가 있었음 (2026-06-06 정정).
 */
data class BrawlerResultStatistics(
    val totalBattleCount: Long,
    val items: List<BrawlerResultStatisticsItem>,
) {
    companion object {
        fun create(
            rows: List<BrawlerEventResultCount>,
            gameSlots: GameSlots,
        ): BrawlerResultStatistics {
            if (rows.isEmpty()) {
                return BrawlerResultStatistics(totalBattleCount = 0L, items = emptyList())
            }

            val totalBattleCount: Long =
                gameSlots.totalGameCount(
                    rows
                        .groupBy { it.eventId }
                        .mapValues { (_, eventRows) -> eventRows.sumOf { it.resultCount.totalCount } },
                )

            val brawlerAggregates =
                rows
                    .groupBy { it.brawlerId }
                    .map { (brawlerId, brawlerRows) ->
                        BrawlerResultCount(
                            brawlerId = brawlerId,
                            resultCount = brawlerRows.map { it.resultCount }.reduce(ResultCount::merge),
                            starPlayerCount = brawlerRows.sumOf { it.starPlayerCount },
                        )
                    }
            val tierScores = BrawlerTierScore.create(brawlerAggregates)

            val items =
                brawlerAggregates.map { agg ->
                    val brawlerSlotSum = agg.resultCount.totalCount
                    BrawlerResultStatisticsItem(
                        brawlerId = agg.brawlerId,
                        battleCount = brawlerSlotSum,
                        pickRate = StatsRate.pickRate(brawlerSlotSum, totalBattleCount),
                        winRate = agg.resultCount.winRate,
                        starPlayerRate =
                            StatsRate.starPlayerRate(
                                agg.starPlayerCount,
                                agg.resultCount.totalCount,
                            ),
                        tierScore = tierScores[agg.brawlerId] ?: BrawlerTierScore(50),
                    )
                }
            return BrawlerResultStatistics(
                totalBattleCount = totalBattleCount,
                items = items.sortedByDescending { it.tierScore.value },
            )
        }
    }
}
