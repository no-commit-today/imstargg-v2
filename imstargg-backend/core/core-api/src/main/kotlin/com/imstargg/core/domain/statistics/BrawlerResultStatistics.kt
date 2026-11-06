package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    /** 해당 브롤러가 등장한 슬롯 수 (= 전투 기록 수). 전체 표본은 wrapper 의 `totalBattleCount` 참조. */
    val battleCount: Long,
    val pickRate: Double,
    val winRate: Double,
    val starPlayerRate: Double,
    val tierScore: TierScore,
)

/**
 * 결과형 통계 - 브롤러 단위. 분모·battleCount 의미와 정정 이력은 [StatsRate.pickRate] 가 정본.
 */
data class BrawlerResultStatistics(
    val totalBattleCount: Long,
    val items: List<BrawlerResultStatisticsItem>,
) {
    companion object {
        fun create(rows: List<BrawlerEventResultCount>): BrawlerResultStatistics {
            if (rows.isEmpty()) {
                return BrawlerResultStatistics(totalBattleCount = 0L, items = emptyList())
            }

            val totalBattleCount: Long = StatsRate.resultGameCount(rows.sumOf { it.resultCount.totalCount })

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
            val tierScores =
                TierScore.create(
                    brawlerAggregates.associate { it.brawlerId to it.resultCount },
                    pickRateWeight = true,
                )

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
                        tierScore = tierScores[agg.brawlerId] ?: TierScore(50),
                    )
                }
            return BrawlerResultStatistics(
                totalBattleCount = totalBattleCount,
                items = items.sortedByDescending { it.tierScore.value },
            )
        }
    }
}
