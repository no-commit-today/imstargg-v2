package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerPairRankStatistics
import com.imstargg.core.domain.statistics.BrawlerRankStatistics
import com.imstargg.core.enums.StatsDateRange
import com.imstargg.core.enums.StatsTierRange
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class BattleRankStatisticsService(
    private val clock: Clock,
    private val rankRepository: BattleRankStatisticsRepositoryWithCache,
) {
    fun getBrawlerRankStatistics(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerRankStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return rankRepository.findBrawlerRankStatistics(eventId, tierRange, startDate, endDate)
    }

    fun getBrawlerPairRankStatistics(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerPairRankStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return rankRepository.findBrawlerPairRankStatistics(
            eventId,
            brawlerId,
            pairBrawlerId,
            tierRange,
            startDate,
            endDate,
        )
    }
}
