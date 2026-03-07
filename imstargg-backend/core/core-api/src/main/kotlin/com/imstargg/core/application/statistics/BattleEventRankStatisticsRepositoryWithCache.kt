package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlersRankCount
import com.imstargg.core.enums.StatsTierRange
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class BattleEventRankStatisticsRepositoryWithCache(
    private val battleEventRankStatisticsRepository: BattleEventRankStatisticsRepository,
) {
    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'battleEventBrawlerRank:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
    )
    fun findBrawlerRankCounts(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerRankCount> = battleEventRankStatisticsRepository.findBrawlerRankCounts(eventId, tierRange, startDate, endDate)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'battleEventBrawlerDuoRank:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #startDate",
    )
    fun findBrawlerDuoRankCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlersRankCount> = battleEventRankStatisticsRepository.findBrawlerDuoRankCounts(eventId, brawlerId, tierRange, startDate)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'battleEventBrawlerTrioRank:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #startDate",
    )
    fun findBrawlerTrioRankCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlersRankCount> = battleEventRankStatisticsRepository.findBrawlerTrioRankCounts(eventId, brawlerId, tierRange, startDate)
}
