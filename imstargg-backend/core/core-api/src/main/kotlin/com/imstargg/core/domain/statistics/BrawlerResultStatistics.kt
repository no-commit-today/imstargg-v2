package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    /** 조회 범위 전체의 전투(게임) 수. 분모와 동일. */
    val totalBattleCount: Long,
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
 */
data class BrawlerResultStatistics(
    val totalBattleCount: Long,
    val items: List<BrawlerResultStatisticsItem>,
) {
    companion object {
        fun create(
            rows: List<BrawlerEventResultCount>,
            slotsPerGameByEvent: Map<BrawlStarsId, Int>,
        ): BrawlerResultStatistics {
            if (rows.isEmpty()) {
                return BrawlerResultStatistics(totalBattleCount = 0L, items = emptyList())
            }

            val totalBattleCount: Long =
                rows.groupBy { it.eventId }
                    .map { (eventId, eventRows) ->
                        val eventSlotSum = eventRows.sumOf { it.resultCount.totalCount }
                        val slotsPerGame =
                            slotsPerGameByEvent[eventId]
                                ?: error("playerCounts missing for event=$eventId")
                        eventSlotSum / slotsPerGame
                    }.sum()

            val brawlerAggregates =
                rows.groupBy { it.brawlerId }
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
                        totalBattleCount = totalBattleCount,
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

        @Deprecated("Use create(rows, slotsPerGameByEvent). Removed in Task 9.", level = DeprecationLevel.WARNING)
        fun create(counts: List<BrawlerResultCount>): BrawlerResultStatistics =
            create(
                counts.map { BrawlerEventResultCount(BrawlStarsId(0L), it.brawlerId, it.resultCount, it.starPlayerCount) },
                mapOf(BrawlStarsId(0L) to 1),
            )
    }
}
