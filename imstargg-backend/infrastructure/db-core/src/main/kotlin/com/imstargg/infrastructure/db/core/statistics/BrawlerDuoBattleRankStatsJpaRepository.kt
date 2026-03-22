package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerDuoBattleRankStatsJpaRepository : JpaRepository<BrawlerDuoBattleRankStatsEntity, Long> {
    fun findAllByEventBrawlStarsIdAndPeriodTypeAndTierRangeAndDateAndBrawlerBrawlStarsId(
        eventBrawlStarsId: Long,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        date: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerDuoBattleRankStatsEntity>

    fun findAllByPeriodTypeAndDateBetween(
        periodType: StatsPeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerDuoBattleRankStatsEntity>
}
