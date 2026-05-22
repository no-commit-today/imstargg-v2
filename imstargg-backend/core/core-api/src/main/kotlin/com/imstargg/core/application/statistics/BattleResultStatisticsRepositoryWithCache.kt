package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerPairResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.enums.StatsTierRange
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class BattleResultStatisticsRepositoryWithCache(
    private val battleResultStatisticsRepository: BattleResultStatisticsRepository,
) {
    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:brawlerResultStats:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultStatistics = battleResultStatisticsRepository.findBrawlerResultStatistics(eventId, tierRange, startDate, endDate)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:brawlerPairResultStats:' + #eventId + ':' + #brawlerId + ':' + #pairBrawlerId" +
                " + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerPairResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerPairResultStatistics =
        battleResultStatisticsRepository.findBrawlerPairResultStatistics(
            eventId,
            brawlerId,
            pairBrawlerId,
            tierRange,
            startDate,
            endDate,
        )

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:brawlerEnemyResultStats:' + #eventId + ':' + #brawlerId + ':' + #tierRange" +
                " + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerEnemyResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerEnemyResultStatistics =
        battleResultStatisticsRepository.findBrawlerEnemyResultStatistics(
            eventId,
            brawlerId,
            tierRange,
            startDate,
            endDate,
        )

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:brawlerDailyResultStats:' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultDailyStatistics =
        battleResultStatisticsRepository.findBrawlerDailyResultStatistics(
            brawlerId,
            tierRange,
            startDate,
            endDate,
        )
}
