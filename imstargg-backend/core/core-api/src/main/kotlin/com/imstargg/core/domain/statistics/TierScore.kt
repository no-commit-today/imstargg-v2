package com.imstargg.core.domain.statistics

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 티어 점수. 50 이 기준 평균 승률에 해당하고, 클수록 그보다 낫다. 등급은 [grade] 로 구한다.
 *
 * 점수 = `50 + (수축 승률 - 기준 평균 승률) × SCALING_K × 가중`.
 * 수축 승률은 기준 평균 승률 쪽으로 `BAYESIAN_C` 판만큼 당긴 승률이라 표본이 적은 키일수록 50 에 붙는다.
 * 기준 평균 승률은 [create] 에 `baselineWinRate` 를 주면 그 값(조합은 [ComboStatsPolicy.COMBO_BASELINE_WIN_RATE]),
 * 주지 않으면 입력 전체의 `Σ승 / Σ(승 + 패)` 다.
 * 가중은 [create] 의 `pickRateWeight` 가 켜져 있을 때만
 * `MIN_WEIGHT_BETA + (1 − MIN_WEIGHT_BETA)·ln(1 + LOG_ALPHA·픽률)/ln(1 + LOG_ALPHA·최대 픽률)` 로 정해지고,
 * 꺼져 있으면 1.0 이다.
 *
 * ## 저장하지 않는 이유
 * 사전 집계 컬럼·테이블이 없고 조회 시 계산하며, 결과는 Redis `statistics` 캐시에 30분 보관된다.
 * 그래서 산식·클래스 변경은 캐시 키 버전 범프로 처리한다(2026.05.3 사전집계 → 즉석 계산 전환).
 * 1인 통계의 산식은 키 하나의 값이 아니라 **조회 기간 전체**에서 정해진다. 수축의 기준인 평균 승률과
 * 픽률 가중의 기준인 최대 픽률이 모두 그 기간의 모든 키에 걸쳐 나오므로, 기간이 달라지면 같은 키의 점수도 달라진다.
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
         *   끄면 전체 표본이 0 이어도 모든 키가 50 이 된다. n 이 0 인 키의 수축 승률은 기준 평균 승률과 같아
         *   차이가 0 이기 때문이다.
         * @param baselineWinRate 수축 기준 평균 승률. 주면 입력 평균 대신 이 값을 쓰고, 없으면
         *   `Σ승 / Σ(승 + 패)` 를 쓴다. 조합(2인·3인)은 [ComboStatsPolicy.COMBO_BASELINE_WIN_RATE] 를 준다.
         */
        fun <K> create(
            counts: Map<K, ResultCount>,
            pickRateWeight: Boolean,
            baselineWinRate: Double? = null,
        ): Map<K, TierScore> {
            if (counts.isEmpty()) return emptyMap()

            val totalBattleCount = counts.values.sumOf { it.totalCount }
            val totalWins = counts.values.sumOf { it.victoryCount }
            val totalLosses = counts.values.sumOf { it.defeatCount }
            val avgWinRate = baselineWinRate ?: StatsRate.winRate(totalWins, totalLosses)

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
