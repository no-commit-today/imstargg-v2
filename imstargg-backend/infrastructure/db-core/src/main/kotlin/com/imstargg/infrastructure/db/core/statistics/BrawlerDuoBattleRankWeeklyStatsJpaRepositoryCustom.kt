package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface BrawlerDuoBattleRankWeeklyStatsJpaRepositoryCustom {
    fun findByEventAndTierRangeAndDateAndBrawlerBrawlStarsId(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        date: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerDuoBattleRankWeeklyStatsEntity>
}
