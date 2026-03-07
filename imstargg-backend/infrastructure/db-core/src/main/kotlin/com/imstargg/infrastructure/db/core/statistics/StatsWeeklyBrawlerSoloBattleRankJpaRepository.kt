package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface StatsWeeklyBrawlerSoloBattleRankJpaRepository : JpaRepository<StatsWeeklyBrawlerSoloBattleRankEntity, Long> {
    fun findByEventBrawlStarsIdAndTierRangeAndStartDate(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<StatsWeeklyBrawlerSoloBattleRankEntity>
}
