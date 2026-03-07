package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

data class BattleEventBrawlerStatisticsParam(
    val eventId: BrawlStarsId,
    val tierRange: StatsTierRange,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class BattleEventBrawlersStatisticsParam(
    val eventId: BrawlStarsId,
    val brawlerId: BrawlStarsId,
    val tierRange: StatsTierRange,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class BrawlerStatisticsParam(
    val tierRange: StatsTierRange,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class BrawlersStatisticsParam(
    val brawlerId: BrawlStarsId,
    val tierRange: StatsTierRange,
    val startDate: LocalDate,
    val endDate: LocalDate,
)
