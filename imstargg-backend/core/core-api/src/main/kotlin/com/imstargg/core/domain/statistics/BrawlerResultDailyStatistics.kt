package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import java.time.LocalDate

data class BrawlerResultDailyStatistics(
    val date: LocalDate,
    val brawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val winRate: Double,
    val starPlayerRate: Double,
)
