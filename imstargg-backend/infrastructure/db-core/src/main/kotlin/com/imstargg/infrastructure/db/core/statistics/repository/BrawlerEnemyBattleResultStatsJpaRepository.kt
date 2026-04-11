package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerEnemyBattleResultStatsJpaRepository : JpaRepository<BrawlerEnemyBattleResultStatsEntity, Long> {
    fun findAllByEventBrawlStarsIdAndTierRangeAndPeriodTypeInAndBattleDateBetween(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        periodTypes: Collection<StatsPeriodType>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>

    fun findAllByEventBrawlStarsIdAndBrawlerBrawlStarsIdAndTierRangeAndPeriodTypeInAndBattleDateBetween(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        periodTypes: Collection<StatsPeriodType>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>

    fun findAllByBrawlerBrawlStarsIdAndTierRangeAndPeriodTypeInAndBattleDateBetween(
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        periodTypes: Collection<StatsPeriodType>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>

    fun findAllByTierRangeAndPeriodTypeInAndBattleDateBetween(
        tierRange: StatsTierRange,
        periodTypes: Collection<StatsPeriodType>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>
}
