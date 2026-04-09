package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import java.time.LocalDate

interface BrawlerTeamBattleResultStatsJpaRepositoryCustom {
    fun findAllByConditions(
        eventBrawlStarsId: Long?,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        startBattleDate: LocalDate,
        brawlerBrawlStarsId: Long?,
        teamSize: StatsTeamSize,
    ): List<BrawlerTeamBattleResultStatsEntity>
}
