package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.domain.statistics.BrawlerTeamResultStatistics
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BattleResultStatisticsService(
    private val resultRepository: BattleResultStatisticsRepositoryWithCache,
) {
    fun getBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultStatistics {
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
        brawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        teamSize: StatsTeamSize,
    ): BrawlerTeamResultStatistics =
        BrawlerTeamResultStatistics.create(
            resultRepository.findBrawlerTeamResultCounts(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                teamSize = teamSize,
            ),
        )

    fun getEnemyResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): BrawlerEnemyResultStatistics {
        val counts =
            resultRepository.findBrawlerEnemyResultCounts(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
            )
        return BrawlerEnemyResultStatistics.create(counts)
    }

    fun getDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultDailyStatistics> =
        resultRepository
            .findBrawlerDailyResultCounts(
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            ).map { BrawlerResultDailyStatistics.create(it) }
}
