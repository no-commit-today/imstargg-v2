package com.imstargg.infrastructure.db.core.statistics

data class BrawlerBattleResultAggregateProjection(
    val brawlerBrawlStarsId: Long,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
    val starPlayerCount: Long,
)
