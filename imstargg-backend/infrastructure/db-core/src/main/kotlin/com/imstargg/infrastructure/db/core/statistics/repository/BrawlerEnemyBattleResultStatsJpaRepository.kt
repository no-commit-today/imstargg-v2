package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerEnemyBattleResultStatsJpaRepository : JpaRepository<BrawlerEnemyBattleResultStatsEntity, Long> {
    fun findAllByEventBrawlStarsIdAndPeriodTypeAndTierRangeAndDate(
        eventBrawlStarsId: Long,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>

    fun findAllByEventBrawlStarsIdAndBrawlerBrawlStarsIdAndPeriodTypeAndTierRangeAndDate(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>

    fun findAllByPeriodTypeAndDateBetween(
        periodType: StatsPeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>
}
