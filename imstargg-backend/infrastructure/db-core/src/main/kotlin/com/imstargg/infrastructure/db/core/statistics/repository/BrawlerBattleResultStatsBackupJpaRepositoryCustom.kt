package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface BrawlerBattleResultStatsBackupJpaRepositoryCustom {
    fun findGroupedByEventAndBrawler(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEventResultProjection>
}
