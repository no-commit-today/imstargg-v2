package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.tanh

/**
 * 1인 브롤러 티어 점수. 50 이 기준 평균 승률에 해당하고, 클수록 그보다 낫다.
 * 등급([grade])은 전투 수가 [GRADE_MIN_BATTLE_COUNT] 이상일 때만 있다.
 *
 * 점수 = `round(max(0, 50 + SCALING_K × (수축 승률 − 평균 승률) + 가산))`.
 * - 수축 승률 = `(n × 승률 + BAYESIAN_C × 평균 승률) / (n + BAYESIAN_C)`. 표본이 적을수록 평균 승률 쪽으로 당겨진다.
 *   n 은 승 + 패 + 무다.
 * - 평균 승률(μ) = 입력 전체의 `Σ승 / Σ(승 + 패)`.
 * - 가산 = `PICK_RATE_BONUS_CAP × tanh(ln(n / 중앙값) / ln(PICK_RATE_BONUS_SATURATION))`.
 *   픽률 ÷ 픽률 중앙값의 로그를 tanh 로 ±4점에 묶은 값이며, 전투 수 ÷ 전투 수 중앙값과 같다.
 *   중앙값이 0 이면 0, 중앙값이 0 보다 큰데 n 이 0 이면 −4 다.
 *
 * ## 상수를 이렇게 둔 이유
 * 가산 상한 4 는 A 경계까지의 거리(5)보다 작아, 인기만으로는 등급이 바뀌지 않는다.
 * 표본 하한 300 은 수축 승률이 자기 승률과 평균 승률의 중간이 되는 판수(`BAYESIAN_C`)다.
 *
 * ## 맥락에 따라 점수가 바뀐다
 * μ 와 중앙값이 조회 맥락(이벤트, 티어 구간, 기간)의 모든 브롤러에서 나오므로,
 * 맥락이 바뀌면 같은 브롤러의 점수도 바뀐다.
 *
 * ## 저장하지 않는다
 * 사전 집계 없이 조회 때 계산한다. 결과는 Redis `statistics` 캐시에 30분 보관되므로,
 * 산식이나 타입을 바꾸면 캐시 키 버전을 올린다.
 *
 * ## 결정 이력
 * 2026-11 스펙 C2 가 픽률을 곱하던 가중을 버리고 가산으로 바꿨고,
 * 등급 경계를 55/52/48/45 에서 60/55/45/40 으로 넓혔고, 표본 하한을 새로 뒀다. 근거 수치는 스펙 부록 C2.
 */
data class BrawlerTierScore(
    val value: Int,
    val grade: TierGrade?,
) {
    companion object {
        private const val BAYESIAN_C = 300.0
        private const val SCALING_K = 200.0
        private const val PICK_RATE_BONUS_CAP = 4.0
        private const val PICK_RATE_BONUS_SATURATION = 3.0
        private const val GRADE_MIN_BATTLE_COUNT = 300L

        fun create(counts: Map<BrawlStarsId, ResultCount>): Map<BrawlStarsId, BrawlerTierScore> {
            if (counts.isEmpty()) return emptyMap()

            val averageWinRate =
                StatsRate.winRate(
                    victoryCount = counts.values.sumOf { it.victoryCount },
                    defeatCount = counts.values.sumOf { it.defeatCount },
                )
            val median = median(counts.values.map { it.totalCount })

            return counts.mapValues { (_, resultCount) ->
                val n = resultCount.totalCount.toDouble()
                val adjustedWinRate = (n * resultCount.winRate + BAYESIAN_C * averageWinRate) / (n + BAYESIAN_C)
                val bonus =
                    when {
                        median == 0.0 -> 0.0
                        n == 0.0 -> -PICK_RATE_BONUS_CAP
                        else -> PICK_RATE_BONUS_CAP * tanh(ln(n / median) / ln(PICK_RATE_BONUS_SATURATION))
                    }
                val value = max(0.0, 50.0 + SCALING_K * (adjustedWinRate - averageWinRate) + bonus).roundToInt()
                BrawlerTierScore(value = value, grade = gradeOf(value = value, battleCount = resultCount.totalCount))
            }
        }

        fun gradeOf(
            value: Int,
            battleCount: Long,
        ): TierGrade? =
            when {
                battleCount < GRADE_MIN_BATTLE_COUNT -> null
                value >= 60 -> TierGrade.S
                value >= 55 -> TierGrade.A
                value >= 45 -> TierGrade.B
                value >= 40 -> TierGrade.C
                else -> TierGrade.D
            }

        private fun median(values: List<Long>): Double {
            val sorted = values.sorted()
            val middle = sorted.size / 2
            return if (sorted.size % 2 == 1) {
                sorted[middle].toDouble()
            } else {
                (sorted[middle - 1] + sorted[middle]) / 2.0
            }
        }
    }
}
