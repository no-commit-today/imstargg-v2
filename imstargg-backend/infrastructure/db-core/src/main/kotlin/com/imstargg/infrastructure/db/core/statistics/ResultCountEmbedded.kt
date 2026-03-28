package com.imstargg.infrastructure.db.core.statistics

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class ResultCountEmbedded(
    @Column(name = "victory_count", nullable = false)
    val victoryCount: Long,
    @Column(name = "defeat_count", nullable = false)
    val defeatCount: Long,
    @Column(name = "draw_count", nullable = false)
    val drawCount: Long,
) {
    operator fun plus(other: ResultCountEmbedded): ResultCountEmbedded =
        ResultCountEmbedded(
            victoryCount = victoryCount + other.victoryCount,
            defeatCount = defeatCount + other.defeatCount,
            drawCount = drawCount + other.drawCount,
        )
}
