package com.imstargg.core.api.controller.v1.response

import java.time.LocalDate

data class PlayerBrawlerStatisticsItemResponse(
    val brawler: BrawlerResponse?,
    val totalBattleCount: Long,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
    val winRate: Double,
)

data class PlayerBrawlerStatisticsResponse(
    val startBattleDate: LocalDate,
    val endBattleDate: LocalDate,
    val totalBattleCount: Long,
    val items: List<PlayerBrawlerStatisticsItemResponse>,
)
