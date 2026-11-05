package com.imstargg.infrastructure.db.core.statistics.repository

data class BrawlerPairResultProjection(
    val brawler1BrawlStarsId: Long,
    val brawler2BrawlStarsId: Long,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
)
