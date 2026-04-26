package com.imstargg.infrastructure.db.core.statistics.repository

data class BrawlerPairResultProjection(
    val brawlerBrawlStarsId: Long,
    val pairBrawlerBrawlStarsId: Long,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
)
