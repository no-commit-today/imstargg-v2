package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsEntity
import java.time.LocalDate

interface BrawlerTeamBattleRankStatsJpaRepositoryCustom {
    fun findAllByEventAndConditionsAndBrawlerAndTeamSize(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        brawlerBrawlStarsId: Long,
        teamSize: StatsTeamSize,
    ): List<BrawlerTeamBattleRankStatsEntity>
}
