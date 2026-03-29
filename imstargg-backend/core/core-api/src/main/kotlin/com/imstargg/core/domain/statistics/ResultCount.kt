package com.imstargg.core.domain.statistics

data class ResultCount(
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
) {
    val totalCount: Long get() = victoryCount + defeatCount + drawCount

    val winRate: Double get() = StatsRate.winRate(victoryCount, defeatCount)

    fun merge(other: ResultCount) =
        ResultCount(
            victoryCount = victoryCount + other.victoryCount,
            defeatCount = defeatCount + other.defeatCount,
            drawCount = drawCount + other.drawCount,
        )
}
