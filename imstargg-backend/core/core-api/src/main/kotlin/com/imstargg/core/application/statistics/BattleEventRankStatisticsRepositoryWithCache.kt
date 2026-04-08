package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlerTeamRankCount
import com.imstargg.core.enums.StatsTierRange
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class BattleEventRankStatisticsRepositoryWithCache(
    private val battleEventRankStatisticsRepository: BattleEventRankStatisticsRepository,
) {
    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'battleEventBrawlerRank:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
    )
    fun findBrawlerRankCounts(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerRankCount> = battleEventRankStatisticsRepository.findBrawlerRankCounts(eventId, tierRange, startDate, endDate)

    @Cacheable(
        cacheNames = [CacheNames.STATISTICS],
        key = "'battleEventTeamRank:' + #eventId + ':' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #teamSize",
    )
    fun findBrawlerTeamRankCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        teamSize: Int,
    ): List<BrawlerTeamRankCount> =
        battleEventRankStatisticsRepository.findBrawlerTeamRankCounts(eventId, brawlerId, tierRange, startDate, teamSize)
}
