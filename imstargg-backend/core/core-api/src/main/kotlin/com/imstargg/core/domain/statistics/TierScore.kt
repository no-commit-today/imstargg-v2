package com.imstargg.core.domain.statistics

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 티어 점수. 50 이 같은 조회 기간 안의 평균이고, 클수록 평균보다 낫다. 등급은 [grade] 로 구한다.
 *
 * 점수 = `50 + (수축 승률 - 평균 승률) × SCALING_K × 가중`.
 * 수축 승률은 평균 승률 쪽으로 `BAYESIAN_C` 판만큼 당긴 승률이라 표본이 적은 키일수록 50 에 붙는다.
 * 가중은 [create] 의 `pickRateWeight` 가 켜져 있을 때만 `ln(1 + 100·픽률) / ln(1 + 100·최대 픽률)` 로
 * 정해지고(최소 `MIN_WEIGHT_BETA`), 꺼져 있으면 1.0 이다.
 *
 * ## 저장하지 않는 이유
 * 점수는 DB·캐시 어디에도 저장하지 않고 조회할 때마다 계산한다. 산식이 키 하나의 값이 아니라
 * **조회 기간 전체**에서 정해지기 때문이다. 수축의 기준인 평균 승률과 픽률 가중의 기준인 최대 픽률이
 * 모두 그 기간의 모든 키에 걸쳐 나온다. 기간이 달라지면 같은 키의 점수도 달라지므로 미리 계산해 둘 수 없다.
 * 프로젝트는 2026.05.3 에 사전집계 테이블을 없애고 즉석 계산 + 캐시로 옮겼고, 그래서 산식이 바뀌어도
 * 저장값을 다시 만들 필요 없이 캐시 키 버전만 올리면 된다.
 *
 * ## 조합은 픽률 가중을 끈다
 * 1인 브롤러 통계는 `pickRateWeight = true`(기존 산식 그대로), 2인·3인 조합은 `false` 다.
 * 픽률을 곱하는 가중은 표본이 적어 믿기 어려운 값을 깎는 신뢰도 보정이 아니라, 덜 쓰이는 키를 깎는
 * 인기 감점이다(스펙 A 부록 A.5). 신뢰도는 수축이 이미 맡는다. 반분 검증에서도 가장 잘 맞은 예측기는
 * 수축 승률이었으므로 조합은 수축 승률만으로 점수를 정한다.
 */
data class TierScore(
    val value: Int,
) {
    fun grade(): TierGrade =
        when {
            value >= 55 -> TierGrade.S
            value >= 52 -> TierGrade.A
            value >= 48 -> TierGrade.B
            value >= 45 -> TierGrade.C
            else -> TierGrade.D
        }

    companion object {
        private const val BAYESIAN_C = 300.0
        private const val LOG_ALPHA = 100.0
        private const val SCALING_K = 200.0
        private const val MIN_WEIGHT_BETA = 0.1

        /**
         * @param counts 키(브롤러, 2인 쌍, 3인 셋 등)마다 조회 기간을 합산한 승·패·무
         * @param pickRateWeight 켜면 픽률 가중을 곱하고, 끄면 수축 승률만으로 점수를 정한다.
         *   끄면 최대 픽률을 계산하지 않으므로 전체 표본이 0 이어도 모든 키가 50 이 된다.
         */
        fun <K> create(
            counts: Map<K, ResultCount>,
            pickRateWeight: Boolean,
        ): Map<K, TierScore> {
            if (counts.isEmpty()) return emptyMap()

            val totalBattleCount = counts.values.sumOf { it.totalCount }
            val totalWins = counts.values.sumOf { it.victoryCount }
            val totalLosses = counts.values.sumOf { it.defeatCount }
            val avgWinRate = StatsRate.winRate(totalWins, totalLosses)

            val maxPickRate =
                if (pickRateWeight) {
                    counts.values.maxOf { StatsRate.pickRate(it.totalCount, totalBattleCount) }
                } else {
                    0.0
                }
            if (pickRateWeight && maxPickRate == 0.0) {
                return counts.mapValues { TierScore(50) }
            }

            return counts.mapValues { (_, resultCount) ->
                val n = resultCount.totalCount.toDouble()
                val winRate = resultCount.winRate

                val adjustedWinRate = (n * winRate + BAYESIAN_C * avgWinRate) / (n + BAYESIAN_C)
                val weight =
                    if (pickRateWeight) {
                        val pickRate = StatsRate.pickRate(resultCount.totalCount, totalBattleCount)
                        val relativePickRate = ln(1.0 + LOG_ALPHA * pickRate) / ln(1.0 + LOG_ALPHA * maxPickRate)
                        MIN_WEIGHT_BETA + (1.0 - MIN_WEIGHT_BETA) * relativePickRate
                    } else {
                        1.0
                    }
                val rawScore = 50.0 + (adjustedWinRate - avgWinRate) * SCALING_K * weight

                TierScore(max(0.0, rawScore).roundToInt())
            }
        }
    }
}
