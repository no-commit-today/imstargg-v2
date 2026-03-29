package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleRankStatsEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerBattleRankStatsJpaRepository : JpaRepository<BrawlerBattleRankStatsEntity, Long> {
    fun findAllByEventBrawlStarsIdAndTierRangeAndBattleDateBetween(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerBattleRankStatsEntity>
}
