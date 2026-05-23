package com.imstargg.core.domain.statistics

object StatsRate {
    fun winRate(
        victoryCount: Long,
        defeatCount: Long,
    ): Double {
        val total = victoryCount + defeatCount
        return if (total > 0) victoryCount.toDouble() / total else 0.0
    }

    /**
     * 픽률 = 슬롯(브롤러 등장) 수 / 전투(게임) 수.
     *
     * 분모 `totalBattleCount` 는 **게임 수** 여야 한다 (이전에는 슬롯 합으로 잘못 사용됨 — 2026-05-19 정정).
     * 슬롯 합 ↔ 게임 수 환산은 `GameSlot` 참조.
     */
    fun pickRate(
        battleCount: Long,
        totalBattleCount: Long,
    ): Double = if (totalBattleCount > 0) battleCount.toDouble() / totalBattleCount else 0.0

    fun starPlayerRate(
        starPlayerCount: Long,
        totalBattleCount: Long,
    ): Double = if (totalBattleCount > 0) starPlayerCount.toDouble() / totalBattleCount else 0.0
}
