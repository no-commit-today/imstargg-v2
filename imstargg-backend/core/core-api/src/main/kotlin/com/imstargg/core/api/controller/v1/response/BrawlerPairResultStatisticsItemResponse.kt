package com.imstargg.core.api.controller.v1.response

data class BrawlerPairResultStatisticsItemResponse(
    val brawler: BrawlerResponse?,
    val pairBrawler: BrawlerResponse?,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)
