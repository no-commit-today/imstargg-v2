package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.enums.BrawlerRole
import java.time.LocalDate

data class PlayerBrawlerRoleStatisticsItemResponse(
    val role: BrawlerRole,
    val totalBattleCount: Long,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
    val winRate: Double,
)

data class PlayerBrawlerRoleStatisticsResponse(
    val startBattleDate: LocalDate,
    val endBattleDate: LocalDate,
    val totalBattleCount: Long,
    val items: List<PlayerBrawlerRoleStatisticsItemResponse>,
)
