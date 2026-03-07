package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerEnemyBattleResultWeeklyStatsJpaRepository : JpaRepository<BrawlerEnemyBattleResultWeeklyStatsEntity, Long> {
    fun findByEventBrawlStarsIdAndTierRangeAndDate(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlerEnemyBattleResultWeeklyStatsEntity>

    fun findByEventBrawlStarsIdAndBrawlerBrawlStarsIdAndTierRangeAndDate(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlerEnemyBattleResultWeeklyStatsEntity>
}
