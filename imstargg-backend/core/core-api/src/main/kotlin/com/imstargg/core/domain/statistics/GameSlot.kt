package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.event.BattleEventMode

/**
 * 한 게임당 각 통계 엔티티에 누적되는 슬롯 수.
 *
 * 각 Aggregator 의 집계 로직과 일치해야 한다:
 *  - Result: `BrawlerBattleResultStatsAggregator` 가 양 팀 모든 슬롯을 카운트 → `Σ nᵢ`
 *  - Pair:   `BrawlerPairBattleResultStatsAggregator` 가 한 팀 내 방향성 페어를 카운트 → `Σ nᵢ(nᵢ−1)`
 *  - Enemy:  `BrawlerEnemyBattleResultStatsAggregator` 가 모든 적팀 양방향 매치업을 카운트 → `(Σ nᵢ)² − Σ nᵢ²`
 *
 * 환산: `gameCount = totalSlotSum / GameSlot.<x>SlotsPerGame`
 */
data class GameSlot(val playerCounts: List<Int>) {
    val resultSlotsPerGame: Int
        get() = playerCounts.sum()
    val pairSlotsPerGame: Int
        get() = playerCounts.sumOf { it * (it - 1) }
    val enemySlotsPerGame: Int
        get() {
            val total = playerCounts.sum()
            return total * total - playerCounts.sumOf { it * it }
        }

    companion object {
        fun default(): GameSlot = GameSlot(BattleEventMode.DEFAULT_PLAYER_COUNTS)
    }
}
