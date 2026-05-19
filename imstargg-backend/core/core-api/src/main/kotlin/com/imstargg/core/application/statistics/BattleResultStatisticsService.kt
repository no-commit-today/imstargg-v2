package com.imstargg.core.application.statistics

import com.imstargg.core.application.brawlstars.BattleEventService
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerPairResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.domain.statistics.SlotsPerGame
import com.imstargg.core.enums.StatsDateRange
import com.imstargg.core.enums.StatsTierRange
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

@Service
class BattleResultStatisticsService(
    private val clock: Clock,
    private val resultRepository: BattleResultStatisticsRepositoryWithCache,
    private val battleEventService: BattleEventService,
) {
    fun getBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerResultStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        val rows =
            resultRepository.findBrawlerEventResultCounts(
                eventId = eventId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val slotsPerGameByEvent =
            battleEventService
                .findModesByEventIds(rows.map { it.eventId }.distinct())
                .mapValues { (_, mode) -> SlotsPerGame.forResult(mode.playerCounts) }
        return BrawlerResultStatistics.create(rows = rows, slotsPerGameByEvent = slotsPerGameByEvent)
    }

    fun getBrawlerPairResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerPairResultStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        val raw =
            resultRepository.findBrawlerPairResultRaw(
                eventId = eventId,
                brawlerId = brawlerId,
                pairBrawlerId = pairBrawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val totalBattleCount = computeTotalBattleCount(eventId, tierRange, startDate, endDate)
        return BrawlerPairResultStatistics.create(
            totalBattleCount = totalBattleCount,
            totalElementCount = raw.totalElementCount,
            counts = raw.items,
        )
    }

    fun getEnemyResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerEnemyResultStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        val raw =
            resultRepository.findBrawlerEnemyResultRaw(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val totalBattleCount = computeTotalBattleCount(eventId, tierRange, startDate, endDate)
        return BrawlerEnemyResultStatistics.create(
            totalBattleCount = totalBattleCount,
            totalElementCount = raw.totalElementCount,
            counts = raw.items,
        )
    }

    fun getDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): List<BrawlerResultDailyStatistics> {
        val (startDate, endDate) = dateRange.dateRange(clock)
        return resultRepository
            .findBrawlerDailyResultCounts(
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            ).map { BrawlerResultDailyStatistics.create(it) }
    }

    private fun computeTotalBattleCount(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long {
        val eventSlotSums =
            resultRepository.findEventTotalSlotSums(
                eventId = eventId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val modes = battleEventService.findModesByEventIds(eventSlotSums.map { it.eventId }.distinct())
        return eventSlotSums.sumOf { sum ->
            val mode = modes[sum.eventId] ?: error("playerCounts missing for event=${sum.eventId}")
            sum.totalSlotCount / SlotsPerGame.forResult(mode.playerCounts)
        }
    }
}
