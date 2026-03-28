package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import java.time.LocalDate

interface BrawlerTeamBattleResultStatsJpaRepositoryCustom {
    fun findAllByEventAndConditionsAndBrawler(
        eventBrawlStarsId: Long,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        battleDate: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerTeamBattleResultStatsEntity>

    fun findAllByConditionsAndBrawler(
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        battleDate: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerTeamBattleResultStatsEntity>
}
