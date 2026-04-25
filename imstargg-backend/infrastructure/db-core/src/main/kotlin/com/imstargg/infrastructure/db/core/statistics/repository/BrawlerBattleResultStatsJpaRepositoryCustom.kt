package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface BrawlerBattleResultStatsJpaRepositoryCustom {
    fun findGroupedByBrawler(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultProjection>
}
