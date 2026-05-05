package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
import com.imstargg.core.enums.ResultStatsSortBy
import com.imstargg.core.enums.SortOrder
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
        key = "'brawlerResult:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
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
            "'enemyResult:' + #eventId + ':' + #brawlerId + ':' + #tierRange" +
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
            "'pairResult:' + #eventId + ':' + #brawlerId + ':' + #pairBrawlerId" +
                " + ':' + #tierRange + ':' + #startDate + ':' + #endDate" +
                " + ':' + #sortField + ':' + #sortOrder" +
                " + ':' + #offsetLimit.offset + ':' + #offsetLimit.limit",
        sync = true,
    )
    fun findBrawlerPairResult(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        sortField: ResultStatsSortBy,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): BrawlerPairResultCounts {
        val items =
            battleResultStatisticsRepository.findBrawlerPairResultCounts(
                eventId = eventId,
                brawlerId = brawlerId,
                pairBrawlerId = pairBrawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                sortField = sortField,
                sortOrder = sortOrder,
                offsetLimit = offsetLimit,
            )
        val totalBattleCount =
            battleResultStatisticsRepository.countPairResultTotalBattleCount(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val totalElementCount =
            battleResultStatisticsRepository.countPairResultBy(
                eventId = eventId,
                brawlerId = brawlerId,
                pairBrawlerId = pairBrawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        return BrawlerPairResultCounts(
            items = items,
            totalBattleCount = totalBattleCount,
            totalElementCount = totalElementCount,
        )
    }

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'brawlerDailyResult:' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
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
