package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface BrawlerPairBattleResultStatsJpaRepositoryCustom {
    fun findAllByConditions(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerPairResultProjection>

    fun countDistinctPairBrawlers(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long
}
