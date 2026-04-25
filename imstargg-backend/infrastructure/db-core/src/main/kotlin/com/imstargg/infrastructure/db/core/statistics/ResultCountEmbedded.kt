package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.BattleResult
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
@ConsistentCopyVisibility
data class ResultCountEmbedded internal constructor(
    @Column(name = "victory_count", nullable = false)
    val victoryCount: Long,
    @Column(name = "defeat_count", nullable = false)
    val defeatCount: Long,
    @Column(name = "draw_count", nullable = false)
    val drawCount: Long,
    @Column(name = "total_count", nullable = false)
    val totalCount: Long,
) {
    companion object {
        fun create(
            victoryCount: Long,
            defeatCount: Long,
            drawCount: Long,
        ): ResultCountEmbedded =
            ResultCountEmbedded(
                victoryCount = victoryCount,
                defeatCount = defeatCount,
                drawCount = drawCount,
                totalCount = victoryCount + defeatCount + drawCount,
            )
    }

    operator fun plus(other: ResultCountEmbedded): ResultCountEmbedded =
        ResultCountEmbedded(
            victoryCount = victoryCount + other.victoryCount,
            defeatCount = defeatCount + other.defeatCount,
            drawCount = drawCount + other.drawCount,
            totalCount = totalCount + other.totalCount,
        )

    fun increment(result: BattleResult): ResultCountEmbedded =
        when (result) {
            BattleResult.VICTORY ->
                copy(victoryCount = victoryCount + 1, totalCount = totalCount + 1)
            BattleResult.DEFEAT ->
                copy(defeatCount = defeatCount + 1, totalCount = totalCount + 1)
            BattleResult.DRAW ->
                copy(drawCount = drawCount + 1, totalCount = totalCount + 1)
        }
}
