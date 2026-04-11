package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.ResultStatsSortField
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsTierRange
import com.querydsl.core.Tuple
import java.time.LocalDate

interface BrawlerEnemyBattleResultStatsJpaRepositoryCustom {
    fun findPagedByConditions(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        sortField: ResultStatsSortField,
        sortOrder: SortOrder,
        offset: Long,
        limit: Long,
    ): List<Tuple>

    fun countTotalBattleCount(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long
}
