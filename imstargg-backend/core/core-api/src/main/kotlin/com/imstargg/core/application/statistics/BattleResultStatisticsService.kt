package com.imstargg.core.application.statistics

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.domain.statistics.BrawlerTeamResultStatistics
import com.imstargg.core.enums.ResultStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsDateRange
import com.imstargg.core.enums.StatsTeamSize
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
        val counts =
            resultRepository.findBrawlerResultCounts(
                eventId = eventId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        return BrawlerResultStatistics.create(counts)
    }

    fun getBrawlerTeamResultStatistics(
        eventId: BrawlStarsId?,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
        teamSize: StatsTeamSize,
        sortBy: ResultStatsSortBy,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): BrawlerTeamResultStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        val totalBattleCount =
            resultRepository.countTeamResultTotalBattleCount(
                eventId = eventId,
                brawlerIds = brawlerIds,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
            )
        val totalCount =
            resultRepository.countTeamResultBy(
                eventId = eventId,
                brawlerIds = brawlerIds,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
            )
        val counts =
            resultRepository.findBrawlerTeamResultCounts(
                eventId = eventId,
                brawlerIds = brawlerIds,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
                sortField = sortBy,
                sortOrder = sortOrder,
                offsetLimit = offsetLimit,
            )
        return BrawlerTeamResultStatistics.create(totalBattleCount, totalCount, counts)
    }

    fun getEnemyResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
        sortBy: ResultStatsSortBy,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): BrawlerEnemyResultStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        val totalBattleCount =
            resultRepository.countEnemyResultTotalBattleCount(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val totalCount =
            resultRepository.countEnemyResultBy(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val counts =
            resultRepository.findBrawlerEnemyResultCounts(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                sortField = sortBy,
                sortOrder = sortOrder,
                offsetLimit = offsetLimit,
            )
        return BrawlerEnemyResultStatistics.create(totalBattleCount, totalCount, counts)
    }

    fun getDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): List<BrawlerResultDailyStatistics> {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return resultRepository
            .findBrawlerDailyResultCounts(
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            ).map { BrawlerResultDailyStatistics.create(it) }
    }
}
