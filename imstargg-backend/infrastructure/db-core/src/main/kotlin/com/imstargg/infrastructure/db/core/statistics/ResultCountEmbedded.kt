package com.imstargg.infrastructure.db.core.statistics

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
}
