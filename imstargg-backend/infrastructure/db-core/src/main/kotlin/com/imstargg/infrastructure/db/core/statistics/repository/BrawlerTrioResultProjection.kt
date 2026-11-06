package com.imstargg.infrastructure.db.core.statistics.repository

data class BrawlerTrioResultProjection(
    val brawler1BrawlStarsId: Long,
    val brawler2BrawlStarsId: Long,
    val brawler3BrawlStarsId: Long,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
)
