package com.imstargg.core.application.statistics

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankStatistics
import com.imstargg.core.domain.statistics.BrawlerTeamRankStatistics
import com.imstargg.core.enums.RankStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsDateRange
import com.imstargg.core.enums.StatsTeamSize
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
    ): List<BrawlerRankStatistics> {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return rankRepository
            .findBrawlerRankCounts(eventId, tierRange, startDate, endDate)
            .map { it.toStatistics() }
    }

    fun getBrawlerTeamRankStatistics(
        eventId: BrawlStarsId,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
        teamSize: StatsTeamSize,
        sortBy: RankStatsSortBy,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): BrawlerTeamRankStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        val sortedBrawlerIds = brawlerIds.sortedBy { it.value }
        val counts =
            rankRepository.findBrawlerTeamRank(
                eventId = eventId,
                brawlerIds = sortedBrawlerIds,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
                sortField = sortBy,
                sortOrder = sortOrder,
                offsetLimit = offsetLimit,
            )
        return BrawlerTeamRankStatistics(
            totalBattleCount = counts.totalBattleCount,
            totalElementCount = counts.totalElementCount,
            items = counts.items,
        )
    }
}
