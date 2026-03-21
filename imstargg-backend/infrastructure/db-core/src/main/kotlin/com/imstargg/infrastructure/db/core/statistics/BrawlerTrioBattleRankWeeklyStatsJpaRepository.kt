package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerTrioBattleRankWeeklyStatsJpaRepository : JpaRepository<BrawlerTrioBattleRankWeeklyStatsEntity, Long> {
    fun findAllByEventBrawlStarsIdAndTierRangeAndDateAndBrawlerBrawlStarsId(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        date: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerTrioBattleRankWeeklyStatsEntity>
}
