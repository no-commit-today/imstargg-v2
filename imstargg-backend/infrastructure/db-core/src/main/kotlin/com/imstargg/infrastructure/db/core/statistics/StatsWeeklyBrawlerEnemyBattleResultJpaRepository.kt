package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface StatsWeeklyBrawlerEnemyBattleResultJpaRepository : JpaRepository<StatsWeeklyBrawlerEnemyBattleResultEntity, Long> {
    fun findByEventBrawlStarsIdAndTierRangeAndStartDate(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<StatsWeeklyBrawlerEnemyBattleResultEntity>

    fun findByEventBrawlStarsIdAndBrawlerBrawlStarsIdAndTierRangeAndStartDate(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<StatsWeeklyBrawlerEnemyBattleResultEntity>
}
