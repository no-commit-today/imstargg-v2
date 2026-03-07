package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlersResultCount
import com.imstargg.core.enums.StatsTierRange
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class BrawlerResultStatisticsRepositoryWithCache(
    private val brawlerResultStatisticsRepository: BrawlerResultStatisticsRepository,
) {
    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'brawlerResult:' + #tierRange + ':' + #startDate + ':' + #endDate",
    )
    fun findBrawlerResultCounts(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultCount> = brawlerResultStatisticsRepository.findBrawlerResultCounts(tierRange, startDate, endDate)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'brawlerDuoResult:' + #brawlerId + ':' + #tierRange + ':' + #startDate",
    )
    fun findBrawlerDuoResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlersResultCount> = brawlerResultStatisticsRepository.findBrawlerDuoResultCounts(brawlerId, tierRange, startDate)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'brawlerTrioResult:' + #brawlerId + ':' + #tierRange + ':' + #startDate",
    )
    fun findBrawlerTrioResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlersResultCount> = brawlerResultStatisticsRepository.findBrawlerTrioResultCounts(brawlerId, tierRange, startDate)
}
