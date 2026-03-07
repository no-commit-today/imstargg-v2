package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface StatsWeeklyBrawlerBattleResultJpaRepositoryCustom {
    fun aggregateByBrawler(
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlerBattleResultAggregateProjection>
}
