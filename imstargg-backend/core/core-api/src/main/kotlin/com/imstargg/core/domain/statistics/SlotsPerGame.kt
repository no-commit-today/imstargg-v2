package com.imstargg.core.domain.statistics

/**
 * 한 게임당 통계 엔티티에 누적되는 "슬롯 수" 를 통계 종류별로 도출한다.
 *
 * 각 Aggregator 의 집계 로직과 일치해야 한다:
 *  - Result: `BrawlerBattleResultStatsAggregator` 가 양 팀 모든 슬롯을 카운트 → `Σ nᵢ`
 *  - Pair:   `BrawlerPairBattleResultStatsAggregator` 가 한 팀 내 방향성 페어를 카운트 → `Σ nᵢ(nᵢ−1)`
 *  - Enemy:  `BrawlerEnemyBattleResultStatsAggregator` 가 모든 적팀 양방향 매치업을 카운트 → `(Σ nᵢ)² − Σ nᵢ²`
 *
 * 환산: `gameCount = totalSlotSum / slotsPerGame(playerCounts)`
 */
object SlotsPerGame {
    fun forResult(playerCounts: List<Int>): Int = playerCounts.sum()

    fun forPair(playerCounts: List<Int>): Int = playerCounts.sumOf { it * (it - 1) }

    fun forEnemy(playerCounts: List<Int>): Int {
        val total = playerCounts.sum()
        return total * total - playerCounts.sumOf { it * it }
    }
}
