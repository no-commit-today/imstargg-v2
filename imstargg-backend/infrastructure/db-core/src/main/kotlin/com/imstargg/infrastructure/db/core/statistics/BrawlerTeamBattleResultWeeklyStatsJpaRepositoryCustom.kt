package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface BrawlerTeamBattleResultWeeklyStatsJpaRepositoryCustom {
    fun findByEventAndTierRangeAndStartDateAndBrawlerBrawlStarsId(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerTeamBattleResultWeeklyStatsEntity>

    fun findByTierRangeAndStartDateAndBrawlerBrawlStarsId(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerTeamBattleResultWeeklyStatsEntity>
}
