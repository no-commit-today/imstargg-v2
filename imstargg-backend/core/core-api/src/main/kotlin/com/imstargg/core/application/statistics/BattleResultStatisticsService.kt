package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.domain.statistics.BrawlerTeamResultStatistics
import com.imstargg.core.enums.StatsTierRange
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BattleResultStatisticsService(
    private val eventResultRepository: BattleEventResultStatisticsRepositoryWithCache,
    private val brawlerResultRepository: BrawlerResultStatisticsRepositoryWithCache,
    private val brawlerTeamResultRepository: BrawlerTeamResultStatisticsRepositoryWithCache,
) {
    fun getBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultStatistics {
        val counts =
            if (eventId != null) {
                eventResultRepository.findBrawlerResultCounts(eventId, tierRange, startDate, endDate)
            } else {
                brawlerResultRepository.findBrawlerResultCounts(tierRange, startDate, endDate)
            }
        return BrawlerResultStatistics.create(counts)
    }

    fun getBrawlerTeamResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        teamSize: Int,
    ): BrawlerTeamResultStatistics =
        BrawlerTeamResultStatistics.create(
            brawlerTeamResultRepository.findBrawlerTeamResultCounts(
                eventId,
                brawlerId,
                tierRange,
                startDate,
                teamSize,
            ),
        )

    fun getEnemyResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): BrawlerEnemyResultStatistics {
        val counts =
            if (eventId != null) {
                eventResultRepository.findBrawlerEnemyResultCounts(eventId, brawlerId, tierRange, startDate)
            } else {
                brawlerResultRepository.findBrawlerEnemyResultCounts(brawlerId, tierRange, startDate)
            }
        return BrawlerEnemyResultStatistics.create(counts)
    }

    fun getDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultDailyStatistics> =
        brawlerResultRepository
            .findBrawlerDailyResultCounts(brawlerId, tierRange, startDate, endDate)
            .map { BrawlerResultDailyStatistics.create(it) }
}
