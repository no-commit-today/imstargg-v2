package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface BrawlerEnemyBattleResultStatsJpaRepositoryCustom {
    fun findAllByConditions(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEnemyResultProjection>

    fun countDistinctEnemyBrawlers(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long
}
