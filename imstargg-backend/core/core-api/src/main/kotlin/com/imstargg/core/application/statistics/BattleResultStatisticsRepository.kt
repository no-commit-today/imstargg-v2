package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
import com.imstargg.core.domain.statistics.BrawlerTeamResultCount
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerEnemyBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerTeamBattleResultStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleResultStatisticsRepository(
    private val battleResultJpaRepository: BrawlerBattleResultStatsJpaRepository,
    private val enemyBattleResultJpaRepository: BrawlerEnemyBattleResultStatsJpaRepository,
    private val teamBattleResultJpaRepository: BrawlerTeamBattleResultStatsJpaRepository,
) {
    fun findBrawlerResultCounts(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultCount> {
        val entities =
            if (eventId != null) {
                battleResultJpaRepository
                    .findAllByEventBrawlStarsIdAndTierRangeAndBattleDateBetween(
                        eventBrawlStarsId = eventId.value,
                        tierRange = tierRange,
                        startDate = startDate,
                        endDate = endDate,
                    )
            } else {
                battleResultJpaRepository
                    .findAllByTierRangeAndBattleDateBetween(
                        tierRange = tierRange,
                        startDate = startDate,
                        endDate = endDate,
                    )
            }

        return entities
            .groupBy { it.brawlerBrawlStarsId }
            .map { (brawlerBrawlStarsId, grouped) ->
                BrawlerResultCount(
                    brawlerId = BrawlStarsId(brawlerBrawlStarsId),
                    resultCount =
                        ResultCount(
                            grouped.sumOf { it.resultCount.victoryCount },
                            grouped.sumOf { it.resultCount.defeatCount },
                            grouped.sumOf { it.resultCount.drawCount },
                        ),
                    starPlayerCount = grouped.sumOf { it.starPlayerCount },
                )
            }
    }

    fun findBrawlerEnemyResultCounts(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlerEnemyResultCount> {
        val entities =
            when {
                eventId != null && brawlerId != null ->
                    enemyBattleResultJpaRepository
                        .findAllByEventBrawlStarsIdAndBrawlerBrawlStarsIdAndPeriodTypeAndTierRangeAndStartBattleDate(
                            eventBrawlStarsId = eventId.value,
                            brawlerBrawlStarsId = brawlerId.value,
                            periodType = StatsPeriodType.WEEKLY,
                            tierRange = tierRange,
                            startBattleDate = startDate,
                        )
                eventId != null ->
                    enemyBattleResultJpaRepository
                        .findAllByEventBrawlStarsIdAndPeriodTypeAndTierRangeAndStartBattleDate(
                            eventBrawlStarsId = eventId.value,
                            periodType = StatsPeriodType.WEEKLY,
                            tierRange = tierRange,
                            startBattleDate = startDate,
                        )
                brawlerId != null ->
                    enemyBattleResultJpaRepository
                        .findAllByBrawlerBrawlStarsIdAndPeriodTypeAndTierRangeAndStartBattleDate(
                            brawlerBrawlStarsId = brawlerId.value,
                            periodType = StatsPeriodType.WEEKLY,
                            tierRange = tierRange,
                            startBattleDate = startDate,
                        )
                else ->
                    enemyBattleResultJpaRepository
                        .findAllByPeriodTypeAndTierRangeAndStartBattleDate(
                            periodType = StatsPeriodType.WEEKLY,
                            tierRange = tierRange,
                            startBattleDate = startDate,
                        )
            }

        return entities
            .groupBy { Pair(it.brawlerBrawlStarsId, it.enemyBrawlerBrawlStarsId) }
            .map { (key, grouped) ->
                BrawlerEnemyResultCount(
                    brawlerId = BrawlStarsId(key.first),
                    enemyBrawlerId = BrawlStarsId(key.second),
                    resultCount =
                        ResultCount(
                            grouped.sumOf { it.resultCount.victoryCount },
                            grouped.sumOf { it.resultCount.defeatCount },
                            grouped.sumOf { it.resultCount.drawCount },
                        ),
                )
            }
    }

    fun findBrawlerTeamResultCounts(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        teamSize: StatsTeamSize,
    ): List<BrawlerTeamResultCount> {
        val entities =
            teamBattleResultJpaRepository.findAllByConditions(
                eventBrawlStarsId = eventId?.value,
                periodType = StatsPeriodType.WEEKLY,
                tierRange = tierRange,
                startBattleDate = startDate,
                brawlerBrawlStarsId = brawlerId?.value,
                teamSize = teamSize,
            )

        if (brawlerId == null) {
            return entities.map { toTeamResultCount(it) }
        }

        return when (teamSize) {
            StatsTeamSize.SIZE_2 -> extractDuoResultCounts(entities, brawlerId)
            StatsTeamSize.SIZE_3 -> entities.map { toTeamResultCount(it) }
        }
    }

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
                            resultCount =
                                ResultCount(
                                    grouped.sumOf { it.resultCount.victoryCount },
                                    grouped.sumOf { it.resultCount.defeatCount },
                                    grouped.sumOf { it.resultCount.drawCount },
                                ),
                            starPlayerCount = grouped.sumOf { it.starPlayerCount },
                        ),
                )
            }.sortedBy { it.date }
    }

    private fun toTeamResultCount(entity: BrawlerTeamBattleResultStatsEntity): BrawlerTeamResultCount =
        BrawlerTeamResultCount(
            brawlerIds = entity.brawlerBrawlStarsIds.map { BrawlStarsId(it) },
            resultCount =
                ResultCount(
                    entity.resultCount.victoryCount,
                    entity.resultCount.defeatCount,
                    entity.resultCount.drawCount,
                ),
        )

    private fun extractDuoResultCounts(
        entities: List<BrawlerTeamBattleResultStatsEntity>,
        brawlerId: BrawlStarsId,
    ): List<BrawlerTeamResultCount> {
        val otherBrawlerIdToResultCount = mutableMapOf<BrawlStarsId, ResultCount>()

        for (entity in entities) {
            val resultCount =
                ResultCount(
                    entity.resultCount.victoryCount,
                    entity.resultCount.defeatCount,
                    entity.resultCount.drawCount,
                )

            for (otherId in entity.brawlerBrawlStarsIds.filter { it != brawlerId.value }) {
                val otherBrawlerId = BrawlStarsId(otherId)
                otherBrawlerIdToResultCount.merge(otherBrawlerId, resultCount) { existing, new ->
                    existing.merge(new)
                }
            }
        }

        return otherBrawlerIdToResultCount.map { (otherBrawlerId, resultCount) ->
            BrawlerTeamResultCount(
                brawlerIds = listOf(brawlerId, otherBrawlerId),
                resultCount = resultCount,
            )
        }
    }
}
