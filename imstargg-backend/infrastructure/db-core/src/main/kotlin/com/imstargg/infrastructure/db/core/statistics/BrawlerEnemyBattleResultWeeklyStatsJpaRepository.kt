package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerEnemyBattleResultWeeklyStatsJpaRepository : JpaRepository<BrawlerEnemyBattleResultWeeklyStatsEntity, Long> {
    fun findByEventBrawlStarsIdAndTierRangeAndStartDate(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlerEnemyBattleResultWeeklyStatsEntity>

    fun findByEventBrawlStarsIdAndBrawlerBrawlStarsIdAndTierRangeAndStartDate(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlerEnemyBattleResultWeeklyStatsEntity>
}
