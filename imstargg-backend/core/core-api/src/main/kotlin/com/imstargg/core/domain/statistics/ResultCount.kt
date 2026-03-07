package com.imstargg.core.domain.statistics

data class ResultCount(
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
) {
    val totalCount: Long get() = victoryCount + defeatCount + drawCount

    val winRate: Double
        get() {
            val victoryDefeatCount = victoryCount + defeatCount
            return if (victoryDefeatCount > 0) victoryCount.toDouble() / victoryDefeatCount else 0.0
        }

    fun merge(other: ResultCount) =
        ResultCount(
            victoryCount = victoryCount + other.victoryCount,
            defeatCount = defeatCount + other.defeatCount,
            drawCount = drawCount + other.drawCount,
        )
}
