package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleResultStatsBackupEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerBattleResultStatsBackupJpaRepository :
    JpaRepository<BrawlerBattleResultStatsBackupEntity, Long>,
    BrawlerBattleResultStatsBackupJpaRepositoryCustom {
    fun findAllByEventBrawlStarsIdAndTierRangeAndBattleDateBetween(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerBattleResultStatsBackupEntity>

    fun findAllByTierRangeAndBattleDateBetween(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerBattleResultStatsBackupEntity>

    fun findAllByBrawlerBrawlStarsIdAndTierRangeAndBattleDateBetween(
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerBattleResultStatsBackupEntity>
}
