package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.enums.RankStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class BattleRankStatisticsRepositoryWithCache(
    private val battleRankStatisticsRepository: BattleRankStatisticsRepository,
) {
    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:battleEventBrawlerRank:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
    )
    fun findBrawlerRankCounts(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerRankCount> = battleRankStatisticsRepository.findBrawlerRankCounts(eventId, tierRange, startDate, endDate)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:teamRank:' + #eventId" +
                " + ':' + T(com.imstargg.core.application.statistics.StatisticsCacheKeys).joinIds(#brawlerIds)" +
                " + ':' + #tierRange + ':' + #startDate + ':' + #endDate" +
                " + ':' + #teamSize + ':' + #sortField + ':' + #sortOrder" +
                " + ':' + #offsetLimit.offset + ':' + #offsetLimit.limit",
        sync = true,
    )
    fun findBrawlerTeamRank(
        eventId: BrawlStarsId,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
        sortField: RankStatsSortBy,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): BrawlerTeamRankCounts {
        val items =
            battleRankStatisticsRepository.findBrawlerTeamRankCounts(
                eventId = eventId,
                brawlerIds = brawlerIds,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
                sortField = sortField,
                sortOrder = sortOrder,
                offsetLimit = offsetLimit,
            )
        val totalBattleCount =
            battleRankStatisticsRepository.countTeamRankTotalBattleCount(
                eventId = eventId,
                brawlerIds = brawlerIds,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
            )
        val totalElementCount =
            battleRankStatisticsRepository.countTeamRankBy(
                eventId = eventId,
                brawlerIds = brawlerIds,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
            )
        return BrawlerTeamRankCounts(
            items = items,
            totalBattleCount = totalBattleCount,
            totalElementCount = totalElementCount,
        )
    }
}
