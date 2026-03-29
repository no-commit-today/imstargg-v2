package com.imstargg.core.domain.statistics

object StatsRate {
    fun winRate(
        victoryCount: Long,
        defeatCount: Long,
    ): Double {
        val total = victoryCount + defeatCount
        return if (total > 0) victoryCount.toDouble() / total else 0.0
    }

    fun pickRate(
        battleCount: Long,
        totalBattleCount: Long,
    ): Double = if (totalBattleCount > 0) battleCount.toDouble() / totalBattleCount else 0.0

    fun starPlayerRate(
        starPlayerCount: Long,
        totalBattleCount: Long,
    ): Double = if (totalBattleCount > 0) starPlayerCount.toDouble() / totalBattleCount else 0.0
}
