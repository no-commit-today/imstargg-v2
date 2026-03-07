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
        key = "'battleEventBrawlerRank:' + #eventId + ':' + #tierRange + ':' + #date",
    )
    fun findBrawlerRankCounts(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlerRankCount> = battleEventRankStatisticsRepository.findBrawlerRankCounts(eventId, tierRange, date)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'battleEventBrawlerDuoRank:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #date",
    )
    fun findBrawlerDuoRankCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlersRankCount> = battleEventRankStatisticsRepository.findBrawlerDuoRankCounts(eventId, brawlerId, tierRange, date)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'battleEventBrawlerTrioRank:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #date",
    )
    fun findBrawlerTrioRankCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlersRankCount> = battleEventRankStatisticsRepository.findBrawlerTrioRankCounts(eventId, brawlerId, tierRange, date)
}
