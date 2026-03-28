package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsEntity
import java.time.LocalDate

interface BrawlerTeamBattleRankStatsJpaRepositoryCustom {
    fun findAllByEventAndConditionsAndBrawlerAndTeamSize(
        eventBrawlStarsId: Long,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        battleDate: LocalDate,
        brawlerBrawlStarsId: Long,
        teamSize: Int,
    ): List<BrawlerTeamBattleRankStatsEntity>
}
