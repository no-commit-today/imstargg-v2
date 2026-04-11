package com.imstargg.core.application.statistics

import com.imstargg.core.config.CacheNames
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlerTeamRankCount
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
            "'v1:battleEventTeamRank:' + #eventId + ':' + #brawlerId + ':' + #tierRange" +
                " + ':' + #startDate + ':' + #endDate + ':' + #teamSize",
    )
    fun findBrawlerTeamRankCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
    ): List<BrawlerTeamRankCount> =
        battleRankStatisticsRepository.findBrawlerTeamRankCounts(
            eventId = eventId,
            brawlerId = brawlerId,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            teamSize = teamSize,
        )
}
