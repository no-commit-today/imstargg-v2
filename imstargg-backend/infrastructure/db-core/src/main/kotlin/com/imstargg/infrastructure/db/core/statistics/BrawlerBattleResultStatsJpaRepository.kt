package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BrawlerBattleResultStatsJpaRepository :
    JpaRepository<BrawlerBattleResultStatsEntity, Long>,
    BrawlerBattleResultStatsJpaRepositoryCustom {
    fun findByEventBrawlStarsIdAndTierRangeAndDateBetween(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerBattleResultStatsEntity>
}
