package com.imstargg.core.batch.domain

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

data class BrawlersBattleStatsKey(
    val brawlerBrawlStarsIds: List<Long>,
    val eventBrawlStarsId: Long,
    val tierRange: StatsTierRange,
    val battleDate: LocalDate,
)
