package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface StatsWeeklyBrawlerBattleResultJpaRepository :
    JpaRepository<StatsWeeklyBrawlerBattleResultEntity, Long>,
    StatsWeeklyBrawlerBattleResultJpaRepositoryCustom {
    fun findByEventBrawlStarsIdAndTierRangeAndStartDate(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<StatsWeeklyBrawlerBattleResultEntity>
}
