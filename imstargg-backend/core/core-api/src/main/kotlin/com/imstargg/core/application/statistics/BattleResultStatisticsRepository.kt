package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerEventResultCount
import com.imstargg.core.domain.statistics.BrawlerPairResultCount
import com.imstargg.core.domain.statistics.BrawlerPairResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.core.infrastructure.jpa.toResultCount
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerEnemyBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerPairBattleResultStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleResultStatisticsRepository(
    private val battleResultJpaRepository: BrawlerBattleResultStatsJpaRepository,
    private val enemyBattleResultJpaRepository: BrawlerEnemyBattleResultStatsJpaRepository,
    private val pairBattleResultJpaRepository: BrawlerPairBattleResultStatsJpaRepository,
    private val gameSlotRepository: GameSlotRepository,
) {
    fun findBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultStatistics {
        val rows: List<BrawlerEventResultCount> =
            battleResultJpaRepository
                .findGroupedByEventAndBrawler(
                    eventBrawlStarsId = eventId?.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerEventResultCount(
                        eventId = BrawlStarsId(projection.eventBrawlStarsId),
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        resultCount =
                            ResultCount(
                                victoryCount = projection.victoryCount,
                                defeatCount = projection.defeatCount,
                                drawCount = projection.drawCount,
                            ),
                        starPlayerCount = projection.starPlayerCount,
                    )
                }
        val gameSlots = gameSlotRepository.findAllByEventIds(rows.map { it.eventId })
        return BrawlerResultStatistics.create(rows = rows, gameSlots = gameSlots)
    }

    fun findBrawlerPairResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerPairResultStatistics {
        val counts: List<BrawlerPairResultCount> =
            pairBattleResultJpaRepository
                .findAllByConditions(
                    eventBrawlStarsId = eventId?.value,
                    brawlerBrawlStarsId = brawlerId.value,
                    pairBrawlerBrawlStarsId = pairBrawlerId?.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerPairResultCount(
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        pairBrawlerId = BrawlStarsId(projection.pairBrawlerBrawlStarsId),
                        resultCount =
                            ResultCount(
                                victoryCount = projection.victoryCount,
                                defeatCount = projection.defeatCount,
                                drawCount = projection.drawCount,
                            ),
                    )
                }
        val totalElementCount =
            pairBattleResultJpaRepository.countDistinctPairBrawlers(
                eventBrawlStarsId = eventId?.value,
                brawlerBrawlStarsId = brawlerId.value,
                pairBrawlerBrawlStarsId = pairBrawlerId?.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val totalGameCount = computeTotalGameCount(eventId, tierRange, startDate, endDate)
        return BrawlerPairResultStatistics.create(
            totalBattleCount = totalGameCount,
            totalElementCount = totalElementCount,
            counts = counts,
        )
    }

    fun findBrawlerEnemyResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerEnemyResultStatistics {
        val counts: List<BrawlerEnemyResultCount> =
            enemyBattleResultJpaRepository
                .findAllByConditions(
                    eventBrawlStarsId = eventId?.value,
                    brawlerBrawlStarsId = brawlerId.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerEnemyResultCount(
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        enemyBrawlerId = BrawlStarsId(projection.enemyBrawlerBrawlStarsId),
                        resultCount =
                            ResultCount(
                                victoryCount = projection.victoryCount,
                                defeatCount = projection.defeatCount,
                                drawCount = projection.drawCount,
                            ),
                    )
                }
        val totalElementCount =
            enemyBattleResultJpaRepository.countDistinctEnemyBrawlers(
                eventBrawlStarsId = eventId?.value,
                brawlerBrawlStarsId = brawlerId.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val totalGameCount = computeTotalGameCount(eventId, tierRange, startDate, endDate)
        return BrawlerEnemyResultStatistics.create(
            totalBattleCount = totalGameCount,
            totalElementCount = totalElementCount,
            counts = counts,
        )
    }

    fun findBrawlerDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultDailyStatistics {
        val dailyCounts: List<BrawlerResultDailyCount> =
            battleResultJpaRepository
                .findAllByBrawlerBrawlStarsIdAndTierRangeAndBattleDateBetween(
                    brawlerBrawlStarsId = brawlerId.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).groupBy { it.battleDate }
                .map { (battleDate, grouped) ->
                    BrawlerResultDailyCount(
                        date = battleDate,
                        brawlerResultCount =
                            BrawlerResultCount(
                                brawlerId = brawlerId,
                                resultCount =
                                    grouped
                                        .map { it.resultCount }
                                        .reduce { acc, r -> acc + r }
                                        .toResultCount(),
                                starPlayerCount = grouped.sumOf { it.starPlayerCount },
                            ),
                    )
                }.sortedBy { it.date }
        return BrawlerResultDailyStatistics.create(brawlerId = brawlerId, dailyCounts = dailyCounts)
    }

    /**
     * 조회 범위의 총 게임 수.
     *
     * Pair / Enemy 통계의 분모도 동일한 게임 수를 사용하므로 항상
     * `brawler_battle_result_stats` (Result 슬롯) + [com.imstargg.core.domain.statistics.GameSlot.resultSlotsPerGame] 로 계산한다.
     * 수학적으로 `pair_slot/pairSlotsPerGame == enemy_slot/enemySlotsPerGame == result_slot/resultSlotsPerGame == gameCount`
     * 가 보장되므로 Result 테이블을 단일 진실 출처로 사용한다.
     */
    private fun computeTotalGameCount(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long {
        val sums =
            battleResultJpaRepository.findEventTotalSlotSums(
                eventBrawlStarsId = eventId?.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val gameSlots = gameSlotRepository.findAllByEventIds(sums.map { BrawlStarsId(it.eventBrawlStarsId) })
        return sums.sumOf { projection ->
            projection.totalSlotCount / gameSlots[BrawlStarsId(projection.eventBrawlStarsId)].resultSlotsPerGame
        }
    }
}
