package com.imstargg.core.domain.statistics

import java.time.LocalDate

data class BrawlerResultDailyCount(
    val date: LocalDate,
    val brawlerResultCount: BrawlerResultCount,
)
