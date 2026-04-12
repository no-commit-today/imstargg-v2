package com.imstargg.infrastructure.db.core.statistics.repository

data class BrawlerEnemyResultProjection(
    val brawlerBrawlStarsId: Long,
    val enemyBrawlerBrawlStarsId: Long,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
)
