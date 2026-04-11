package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlerTeamRankStatisticsItem
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
            "'v1:teamRankTotal:' + #eventId + ':' + #brawlerIds + ':' + #tierRange" +
                " + ':' + #startDate + ':' + #endDate + ':' + #teamSize",
    )
    fun countTeamRankTotalBattleCount(
        eventId: BrawlStarsId,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
    ): Long =
        battleRankStatisticsRepository.countTeamRankTotalBattleCount(
            eventId = eventId,
            brawlerIds = brawlerIds,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
        )

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:teamRankCount:' + #eventId + ':' + #brawlerIds + ':' + #tierRange" +
                " + ':' + #startDate + ':' + #endDate + ':' + #teamSize",
    )
    fun countTeamRankBy(
        eventId: BrawlStarsId,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
    ): Long =
        battleRankStatisticsRepository.countTeamRankBy(
            eventId = eventId,
            brawlerIds = brawlerIds,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
        )

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:teamRank:' + #eventId + ':' + #brawlerIds + ':' + #tierRange + ':' + #startDate + ':' + #endDate" +
                " + ':' + #teamSize + ':' + #sortField + ':' + #sortOrder + ':' + #offsetLimit.offset + ':' + #offsetLimit.limit",
    )
    fun findBrawlerTeamRankCounts(
        eventId: BrawlStarsId,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
        sortField: RankStatsSortBy,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): List<BrawlerTeamRankStatisticsItem> =
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
}
