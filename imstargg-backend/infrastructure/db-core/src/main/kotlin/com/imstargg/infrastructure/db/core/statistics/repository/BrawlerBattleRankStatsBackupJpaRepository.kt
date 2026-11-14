package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleRankStatsBackupEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerBattleRankStatsBackupJpaRepository : JpaRepository<BrawlerBattleRankStatsBackupEntity, Long> {
    fun findAllByEventBrawlStarsIdAndTierRangeAndBattleDateBetween(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerBattleRankStatsBackupEntity>
}
