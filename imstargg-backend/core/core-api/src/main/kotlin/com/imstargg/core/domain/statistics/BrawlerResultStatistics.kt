package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerResultStatisticsItem(
    val brawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
    val starPlayerRate: Double,
    val tierScore: BrawlerTierScore,
)

data class BrawlerResultStatistics(
    val totalBattleCount: Long,
    val items: List<BrawlerResultStatisticsItem>,
) {
    companion object {
        fun create(counts: List<BrawlerResultCount>): BrawlerResultStatistics {
            val totalBattleCount = counts.sumOf { it.resultCount.totalCount }
            val tierScores = BrawlerTierScore.create(counts)
            val items =
                counts.map { count ->
                    BrawlerResultStatisticsItem(
                        brawlerId = count.brawlerId,
                        totalBattleCount = count.resultCount.totalCount,
                        pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalBattleCount),
                        winRate = count.resultCount.winRate,
                        starPlayerRate =
                            StatsRate.starPlayerRate(
                                count.starPlayerCount,
                                count.resultCount.totalCount,
                            ),
                        tierScore = tierScores[count.brawlerId] ?: BrawlerTierScore(50),
                    )
                }
            return BrawlerResultStatistics(
                totalBattleCount = totalBattleCount,
                items = items.sortedByDescending { it.tierScore.value },
            )
        }
    }
}
