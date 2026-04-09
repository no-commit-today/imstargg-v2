package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankStatistics
import com.imstargg.core.domain.statistics.BrawlerTeamRankStatistics
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BattleRankStatisticsService(
    private val rankRepository: BattleRankStatisticsRepositoryWithCache,
) {
    fun getBrawlerRankStatistics(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerRankStatistics> =
        rankRepository
            .findBrawlerRankCounts(eventId, tierRange, startDate, endDate)
            .map { it.toStatistics() }

    fun getBrawlerTeamRankStatistics(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        teamSize: StatsTeamSize,
    ): List<BrawlerTeamRankStatistics> =
        rankRepository
            .findBrawlerTeamRankCounts(eventId, brawlerId, tierRange, startDate, teamSize)
            .map { it.toStatistics() }
}
