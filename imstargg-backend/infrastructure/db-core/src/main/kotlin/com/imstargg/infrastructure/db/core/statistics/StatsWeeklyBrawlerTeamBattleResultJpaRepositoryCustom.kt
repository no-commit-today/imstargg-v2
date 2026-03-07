package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface StatsWeeklyBrawlerTeamBattleResultJpaRepositoryCustom {
    fun findByEventAndTierRangeAndStartDateAndBrawlerBrawlStarsId(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<StatsWeeklyBrawlerTeamBattleResultEntity>

    fun findByTierRangeAndStartDateAndBrawlerBrawlStarsId(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<StatsWeeklyBrawlerTeamBattleResultEntity>
}
