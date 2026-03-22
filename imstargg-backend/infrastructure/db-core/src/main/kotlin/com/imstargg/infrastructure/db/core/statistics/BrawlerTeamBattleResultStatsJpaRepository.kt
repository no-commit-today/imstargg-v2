package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerTeamBattleResultStatsJpaRepository : JpaRepository<BrawlerTeamBattleResultStatsEntity, Long> {
    fun findAllByEventBrawlStarsIdAndPeriodTypeAndTierRangeAndDateAndBrawlerBrawlStarsId(
        eventBrawlStarsId: Long,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        date: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerTeamBattleResultStatsEntity>

    fun findAllByPeriodTypeAndTierRangeAndDateAndBrawlerBrawlStarsId(
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        date: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerTeamBattleResultStatsEntity>

    fun findAllByPeriodTypeAndDateBetween(
        periodType: StatsPeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerTeamBattleResultStatsEntity>
}
