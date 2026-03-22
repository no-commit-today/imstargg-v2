package com.imstargg.infrastructure.db.core.statistics

import java.time.LocalDate

data class BrawlerBattleResultDailyAggregateProjection(
    val date: LocalDate,
    val victoryCount: Long,
    val defeatCount: Long,
    val drawCount: Long,
    val starPlayerCount: Long,
)
