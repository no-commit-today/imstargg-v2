package com.imstargg.infrastructure.db.core.statistics.repository

data class BrawlerEventResultProjection(
    val eventBrawlStarsId: Long,
    val brawlerBrawlStarsId: Long,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
    val starPlayerCount: Long,
)
