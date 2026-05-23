package com.imstargg.core.application.statistics

import com.imstargg.core.application.brawlstars.BattleEventRepositoryWithCache
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.event.BattleEventMode
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
    private val battleEventRepositoryWithCache: BattleEventRepositoryWithCache,
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
        val slotsPerGameByEvent = slotsPerGameByEvent(rows.map { it.eventId })
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
    ): BrawlerResultDailyStatistics {
        val (startDate, endDate) = dateRange.dateRange(clock)
        val dailyCounts =
            resultRepository.findBrawlerDailyResultCounts(
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        return BrawlerResultDailyStatistics.create(brawlerId = brawlerId, dailyCounts = dailyCounts)
    }

    /**
     * 조회 범위의 총 게임 수를 도출한다.
     *
     * Pair / Enemy 통계의 분모도 동일한 게임 수를 사용하므로 항상
     * `brawler_battle_result_stats` (Result 슬롯) + `SlotsPerGame.forResult` 로 계산한다.
     * 수학적으로 `pair_slot/forPair == enemy_slot/forEnemy == result_slot/forResult == game_count`
     * 가 보장되므로 Result 테이블을 단일 진실 출처로 사용하는 것이 더 단순하다.
     */
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
        val slotsPerGameByEvent = slotsPerGameByEvent(eventSlotSums.map { it.eventId })
        return eventSlotSums.sumOf { sum ->
            sum.totalSlotCount / slotsPerGameByEvent.getValue(sum.eventId)
        }
    }

    /**
     * 이벤트별 `slotsPerGame` (= playerCounts 합) 맵을 만든다.
     *
     * mode 정보가 누락된 이벤트는 [BattleEventMode.DEFAULT_PLAYER_COUNTS] 로 폴백하여
     * 호출자가 항상 모든 이벤트 ID 에 대해 값을 얻을 수 있도록 보장한다.
     */
    private fun slotsPerGameByEvent(eventIds: List<BrawlStarsId>): Map<BrawlStarsId, Int> =
        eventIds
            .distinct()
            .associateWith { id ->
                val playerCounts =
                    battleEventRepositoryWithCache.find(id)?.mode?.playerCounts
                        ?: BattleEventMode.DEFAULT_PLAYER_COUNTS
                SlotsPerGame.forResult(playerCounts)
            }
}
