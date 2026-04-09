package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerEnemyBattleResultStatsJpaRepository : JpaRepository<BrawlerEnemyBattleResultStatsEntity, Long> {
    fun findAllByEventBrawlStarsIdAndPeriodTypeAndTierRangeAndStartBattleDate(
        eventBrawlStarsId: Long,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        startBattleDate: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>

    fun findAllByEventBrawlStarsIdAndBrawlerBrawlStarsIdAndPeriodTypeAndTierRangeAndStartBattleDate(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        startBattleDate: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>

    fun findAllByBrawlerBrawlStarsIdAndPeriodTypeAndTierRangeAndStartBattleDate(
        brawlerBrawlStarsId: Long,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        startBattleDate: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>

    fun findAllByPeriodTypeAndTierRangeAndStartBattleDate(
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        startBattleDate: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>
}
