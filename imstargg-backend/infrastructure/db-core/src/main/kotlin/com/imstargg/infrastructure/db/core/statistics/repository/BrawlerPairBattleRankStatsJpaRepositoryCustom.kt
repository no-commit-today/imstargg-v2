package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface BrawlerPairBattleRankStatsJpaRepositoryCustom {
    fun findAllByConditions(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerPairRankProjection>

    fun countTotalBattleCount(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long

    fun countDistinctPairBrawlers(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long
}
