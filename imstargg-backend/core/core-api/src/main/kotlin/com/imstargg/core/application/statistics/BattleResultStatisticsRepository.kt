package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerEventResultCount
import com.imstargg.core.domain.statistics.BrawlerPairResultCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
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
) {
    fun findBrawlerEventResultCounts(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEventResultCount> =
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

    fun findEventTotalSlotSums(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<EventTotalSlotSum> =
        battleResultJpaRepository
            .findEventTotalSlotSums(
                eventBrawlStarsId = eventId?.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            ).map { projection ->
                EventTotalSlotSum(
                    eventId = BrawlStarsId(projection.eventBrawlStarsId),
                    totalSlotCount = projection.totalSlotCount,
                )
            }

    fun findBrawlerEnemyResultCounts(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEnemyResultCount> =
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

    fun countEnemyResultBy(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        enemyBattleResultJpaRepository.countDistinctEnemyBrawlers(
            eventBrawlStarsId = eventId?.value,
            brawlerBrawlStarsId = brawlerId.value,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )

    fun findBrawlerPairResultCounts(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerPairResultCount> =
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

    fun countPairResultBy(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        pairBattleResultJpaRepository.countDistinctPairBrawlers(
            eventBrawlStarsId = eventId?.value,
            brawlerBrawlStarsId = brawlerId.value,
            pairBrawlerBrawlStarsId = pairBrawlerId?.value,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )

    fun findBrawlerDailyResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultDailyCount> {
        val entities =
            battleResultJpaRepository
                .findAllByBrawlerBrawlStarsIdAndTierRangeAndBattleDateBetween(
                    brawlerBrawlStarsId = brawlerId.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                )

        return entities
            .groupBy { it.battleDate }
            .map { (battleDate, grouped) ->
                BrawlerResultDailyCount(
                    date = battleDate,
                    brawlerResultCount =
                        BrawlerResultCount(
                            brawlerId = brawlerId,
                            resultCount = grouped.map { it.resultCount }.reduce { acc, r -> acc + r }.toResultCount(),
                            starPlayerCount = grouped.sumOf { it.starPlayerCount },
                        ),
                )
            }.sortedBy { it.date }
    }
}
