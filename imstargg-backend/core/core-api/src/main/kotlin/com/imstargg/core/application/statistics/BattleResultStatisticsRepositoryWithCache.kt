package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
import com.imstargg.core.enums.ResultStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsTeamSize
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
        key = "'v1:brawlerResult:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
        sync = true,
    )
    fun findBrawlerResultCounts(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultCount> =
        battleResultStatisticsRepository.findBrawlerResultCounts(
            eventId = eventId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:enemyResult:' + #eventId + ':' + #brawlerId + ':' + #tierRange" +
                " + ':' + #startDate + ':' + #endDate" +
                " + ':' + #sortField + ':' + #sortOrder" +
                " + ':' + #offsetLimit.offset + ':' + #offsetLimit.limit",
        sync = true,
    )
    fun findBrawlerEnemyResult(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        sortField: ResultStatsSortBy,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): BrawlerEnemyResultCounts {
        val items =
            battleResultStatisticsRepository.findBrawlerEnemyResultCounts(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                sortField = sortField,
                sortOrder = sortOrder,
                offsetLimit = offsetLimit,
            )
        val totalBattleCount =
            battleResultStatisticsRepository.countEnemyResultTotalBattleCount(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val totalElementCount =
            battleResultStatisticsRepository.countEnemyResultBy(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        return BrawlerEnemyResultCounts(
            items = items,
            totalBattleCount = totalBattleCount,
            totalElementCount = totalElementCount,
        )
    }

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:teamResult:' + #eventId" +
                " + ':' + T(com.imstargg.core.application.statistics.StatisticsCacheKeys).joinIds(#brawlerIds)" +
                " + ':' + #tierRange + ':' + #startDate + ':' + #endDate" +
                " + ':' + #teamSize + ':' + #sortField + ':' + #sortOrder" +
                " + ':' + #offsetLimit.offset + ':' + #offsetLimit.limit",
        sync = true,
    )
    fun findBrawlerTeamResult(
        eventId: BrawlStarsId?,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
        sortField: ResultStatsSortBy,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): BrawlerTeamResultCounts {
        val items =
            battleResultStatisticsRepository.findBrawlerTeamResultCounts(
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
            battleResultStatisticsRepository.countTeamResultTotalBattleCount(
                eventId = eventId,
                brawlerIds = brawlerIds,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
            )
        val totalElementCount =
            battleResultStatisticsRepository.countTeamResultBy(
                eventId = eventId,
                brawlerIds = brawlerIds,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
            )
        return BrawlerTeamResultCounts(
            items = items,
            totalBattleCount = totalBattleCount,
            totalElementCount = totalElementCount,
        )
    }

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'v1:brawlerDailyResult:' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
    )
    fun findBrawlerDailyResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultDailyCount> =
        battleResultStatisticsRepository.findBrawlerDailyResultCounts(
            brawlerId = brawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )
}
