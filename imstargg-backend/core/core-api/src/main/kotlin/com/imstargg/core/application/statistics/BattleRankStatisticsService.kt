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
        val items =
            rankRepository
                .findBrawlerRankCounts(eventId, tierRange, startDate, endDate)
                .map { it.toStatisticsItem() }
        return BrawlerRankStatistics(items = items)
    }

    fun getBrawlerPairRankStatistics(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerPairRankStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        val counts =
            rankRepository.findBrawlerPairRank(
                eventId = eventId,
                brawlerId = brawlerId,
                pairBrawlerId = pairBrawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        return BrawlerPairRankStatistics(
            totalBattleCount = counts.totalBattleCount,
            totalElementCount = counts.totalElementCount,
            items = counts.items,
        )
    }
}
