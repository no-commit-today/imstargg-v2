package com.imstargg.core.api.controller.v1.response

data class BrawlerTeamResultStatisticsItemResponse(
    val brawlers: List<BrawlerResponse?>,
    val totalBattleCount: Long,
    val pickRate: Double,
    val winRate: Double,
)
