package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface BrawlerBattleResultStatsJpaRepositoryCustom {
    fun aggregateByBrawler(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerBattleResultAggregateProjection>
}
