package com.imstargg.infrastructure.db.core.statistics.repository

import java.time.LocalDate

data class BrawlerBattleResultDailyAggregateProjection(
    val battleDate: LocalDate,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
    val starPlayerCount: Long,
)
