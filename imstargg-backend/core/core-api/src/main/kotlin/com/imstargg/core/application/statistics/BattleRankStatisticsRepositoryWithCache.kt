package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerPairRankStatistics
import com.imstargg.core.domain.statistics.BrawlerRankStatistics
import com.imstargg.core.enums.StatsTierRange
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class BattleRankStatisticsRepositoryWithCache(
    private val battleRankStatisticsRepository: BattleRankStatisticsRepository,
) {
    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:brawlerRankStats:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerRankStatistics(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerRankStatistics =
        battleRankStatisticsRepository.findBrawlerRankStatistics(
            eventId = eventId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:brawlerPairRankStats:' + #eventId + ':' + #brawlerId + ':' + #pairBrawlerId" +
                " + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerPairRankStatistics(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerPairRankStatistics =
        battleRankStatisticsRepository.findBrawlerPairRankStatistics(
            eventId = eventId,
            brawlerId = brawlerId,
            pairBrawlerId = pairBrawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )
}
