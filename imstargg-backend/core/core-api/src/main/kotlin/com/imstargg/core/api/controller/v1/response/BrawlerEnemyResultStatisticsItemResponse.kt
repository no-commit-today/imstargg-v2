package com.imstargg.core.api.controller.v1.response

data class BrawlerEnemyResultStatisticsItemResponse(
    val brawler: BrawlerResponse?,
    val enemyBrawler: BrawlerResponse?,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)
