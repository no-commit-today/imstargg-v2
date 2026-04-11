package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
import com.imstargg.core.domain.statistics.BrawlerTeamResultCount
import com.imstargg.core.enums.ResultStatsSortField
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
        key = "'v1:enemyResultTotal:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
    )
    fun countEnemyResultTotalBattleCount(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        battleResultStatisticsRepository.countEnemyResultTotalBattleCount(
            eventId = eventId,
            brawlerId = brawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:enemyResult:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate" +
                " + ':' + #sortField + ':' + #sortOrder + ':' + #offsetLimit.offset + ':' + #offsetLimit.limit",
    )
    fun findBrawlerEnemyResultCounts(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        sortField: ResultStatsSortField,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): Slice<BrawlerEnemyResultCount> =
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

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:teamResultTotal:' + #eventId + ':' + #brawlerId + ':' + #tierRange" +
                " + ':' + #startDate + ':' + #endDate + ':' + #teamSize",
    )
    fun countTeamResultTotalBattleCount(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
    ): Long =
        battleResultStatisticsRepository.countTeamResultTotalBattleCount(
            eventId = eventId,
            brawlerId = brawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
        )

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key =
            "'v1:teamResult:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate" +
                " + ':' + #teamSize + ':' + #sortField + ':' + #sortOrder + ':' + #offsetLimit.offset + ':' + #offsetLimit.limit",
    )
    fun findBrawlerTeamResultCounts(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
        sortField: ResultStatsSortField,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): Slice<BrawlerTeamResultCount> =
        battleResultStatisticsRepository.findBrawlerTeamResultCounts(
            eventId = eventId,
            brawlerId = brawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
            sortField = sortField,
            sortOrder = sortOrder,
            offsetLimit = offsetLimit,
        )

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
