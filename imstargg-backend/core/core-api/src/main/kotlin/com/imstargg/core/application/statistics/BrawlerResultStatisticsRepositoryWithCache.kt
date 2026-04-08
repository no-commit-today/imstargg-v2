package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
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
        key = "'brawlerDailyResult:' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
    )
    fun findBrawlerDailyResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultDailyCount> =
        brawlerResultStatisticsRepository.findBrawlerDailyResultCounts(brawlerId, tierRange, startDate, endDate)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'brawlerEnemyResult:' + #brawlerId + ':' + #tierRange + ':' + #startDate",
    )
    fun findBrawlerEnemyResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlerEnemyResultCount> = brawlerResultStatisticsRepository.findBrawlerEnemyResultCounts(brawlerId, tierRange, startDate)
}
