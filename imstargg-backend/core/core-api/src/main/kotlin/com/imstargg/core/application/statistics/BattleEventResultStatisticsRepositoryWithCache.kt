package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.enums.StatsTierRange
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class BattleEventResultStatisticsRepositoryWithCache(
    private val battleEventResultStatisticsRepository: BattleEventResultStatisticsRepository,
) {
    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:battleEventBrawlerResult:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
    )
    fun findBrawlerResultCounts(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultCount> = battleEventResultStatisticsRepository.findBrawlerResultCounts(eventId, tierRange, startDate, endDate)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:battleEventBrawlerEnemyResult:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #startDate",
    )
    fun findBrawlerEnemyResultCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlerEnemyResultCount> =
        battleEventResultStatisticsRepository.findBrawlerEnemyResultCounts(
            eventId,
            brawlerId,
            tierRange,
            startDate,
        )
}
