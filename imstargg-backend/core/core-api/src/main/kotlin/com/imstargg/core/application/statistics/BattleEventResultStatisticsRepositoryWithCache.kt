package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlersResultCount
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
        key = "'battleEventBrawlerResult:' + #eventId + ':' + #tierRange + ':' + #date",
    )
    fun findBrawlerResultCounts(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlerResultCount> = battleEventResultStatisticsRepository.findBrawlerResultCounts(eventId, tierRange, date)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'battleEventBrawlerDuoResult:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #date",
    )
    fun findBrawlerDuoResultCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlersResultCount> = battleEventResultStatisticsRepository.findBrawlerDuoResultCounts(eventId, brawlerId, tierRange, date)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'battleEventBrawlerTrioResult:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #date",
    )
    fun findBrawlerTrioResultCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlersResultCount> = battleEventResultStatisticsRepository.findBrawlerTrioResultCounts(eventId, brawlerId, tierRange, date)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'battleEventBrawlerEnemyResult:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #date",
    )
    fun findBrawlerEnemyResultCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlerEnemyResultCount> =
        battleEventResultStatisticsRepository.findBrawlerEnemyResultCounts(
            eventId,
            brawlerId,
            tierRange,
            date,
        )
}
