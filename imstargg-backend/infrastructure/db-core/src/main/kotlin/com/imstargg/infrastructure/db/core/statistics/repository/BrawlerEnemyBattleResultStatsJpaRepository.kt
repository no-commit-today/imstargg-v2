package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerEnemyBattleResultStatsJpaRepository : JpaRepository<BrawlerEnemyBattleResultStatsEntity, Long> {
    fun findAllByEventBrawlStarsIdAndPeriodTypeAndTierRangeAndBattleDate(
        eventBrawlStarsId: Long,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        battleDate: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>

    fun findAllByEventBrawlStarsIdAndBrawlerBrawlStarsIdAndPeriodTypeAndTierRangeAndBattleDate(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        battleDate: LocalDate,
    ): List<BrawlerEnemyBattleResultStatsEntity>
}
