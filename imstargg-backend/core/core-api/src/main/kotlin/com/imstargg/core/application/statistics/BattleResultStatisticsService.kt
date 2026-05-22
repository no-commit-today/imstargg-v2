package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerPairResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.enums.StatsDateRange
import com.imstargg.core.enums.StatsTierRange
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class BattleResultStatisticsService(
    private val clock: Clock,
    private val resultRepository: BattleResultStatisticsRepositoryWithCache,
) {
    fun getBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerResultStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return resultRepository.findBrawlerResultStatistics(
            eventId = eventId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )
    }

    fun getBrawlerPairResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerPairResultStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return resultRepository.findBrawlerPairResultStatistics(
            eventId = eventId,
            brawlerId = brawlerId,
            pairBrawlerId = pairBrawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )
    }

    fun getEnemyResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerEnemyResultStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return resultRepository.findBrawlerEnemyResultStatistics(
            eventId = eventId,
            brawlerId = brawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )
    }

    fun getDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerResultDailyStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return resultRepository.findBrawlerDailyResultStatistics(
            brawlerId = brawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )
    }
}
