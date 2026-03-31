package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

data class BrawlerTierScore(
    val value: Int,
) {
    fun tier(): BrawlerTier =
        when {
            value >= 55 -> BrawlerTier.S
            value >= 52 -> BrawlerTier.A
            value >= 48 -> BrawlerTier.B
            value >= 45 -> BrawlerTier.C
            else -> BrawlerTier.D
        }

    companion object {
        private const val BAYESIAN_C = 300.0
        private const val LOG_ALPHA = 100.0
        private const val SCALING_K = 200.0
        private const val MIN_WEIGHT_BETA = 0.1

        fun create(
            counts: List<BrawlerResultCount>,
        ): Map<BrawlStarsId, BrawlerTierScore> {
            if (counts.isEmpty()) return emptyMap()

            val totalBattleCount = counts.sumOf { it.resultCount.totalCount }
            val totalWins = counts.sumOf { it.resultCount.victoryCount }
            val totalLosses = counts.sumOf { it.resultCount.defeatCount }
            val avgWinRate = StatsRate.winRate(totalWins, totalLosses)

            val maxPickRate =
                counts.maxOf {
                    StatsRate.pickRate(it.resultCount.totalCount, totalBattleCount)
                }
            if (maxPickRate == 0.0) {
                return counts.associate { it.brawlerId to BrawlerTierScore(50) }
            }

            return counts.associate { count ->
                val n = count.resultCount.totalCount.toDouble()
                val winRate = count.resultCount.winRate
                val pickRate = StatsRate.pickRate(count.resultCount.totalCount, totalBattleCount)

                val adjustedWinRate = (n * winRate + BAYESIAN_C * avgWinRate) / (n + BAYESIAN_C)
                val pickRateWeight = ln(1.0 + LOG_ALPHA * pickRate) / ln(1.0 + LOG_ALPHA * maxPickRate)
                val rawScore =
                    50.0 + (adjustedWinRate - avgWinRate) * SCALING_K *
                        (MIN_WEIGHT_BETA + (1.0 - MIN_WEIGHT_BETA) * pickRateWeight)
                val score = max(0.0, rawScore).roundToInt()

                count.brawlerId to BrawlerTierScore(score)
            }
        }
    }
}
