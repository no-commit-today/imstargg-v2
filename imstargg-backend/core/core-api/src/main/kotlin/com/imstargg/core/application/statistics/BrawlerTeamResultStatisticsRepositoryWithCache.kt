package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerTeamResultCount
import com.imstargg.core.enums.StatsTierRange
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class BrawlerTeamResultStatisticsRepositoryWithCache(
    private val brawlerTeamResultStatisticsRepository: BrawlerTeamResultStatisticsRepository,
) {
    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'teamResult:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #teamSize",
    )
    fun findBrawlerTeamResultCounts(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        teamSize: Int,
    ): List<BrawlerTeamResultCount> =
        brawlerTeamResultStatisticsRepository.findBrawlerTeamResultCounts(eventId, brawlerId, tierRange, startDate, teamSize)
}
