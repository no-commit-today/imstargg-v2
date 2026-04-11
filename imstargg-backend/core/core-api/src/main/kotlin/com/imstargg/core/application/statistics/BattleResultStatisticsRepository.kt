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
        endDate: LocalDate,
    ): List<BrawlerEnemyResultCount> {
        val allPeriodTypes = StatsPeriodType.entries
        val entities =
            when {
                eventId != null && brawlerId != null ->
                    enemyBattleResultJpaRepository
                        .findAllByEventBrawlStarsIdAndBrawlerBrawlStarsIdAndTierRangeAndPeriodTypeInAndBattleDateBetween(
                            eventBrawlStarsId = eventId.value,
                            brawlerBrawlStarsId = brawlerId.value,
                            tierRange = tierRange,
                            periodTypes = allPeriodTypes,
                            startDate = startDate,
                            endDate = endDate,
                        )
                eventId != null ->
                    enemyBattleResultJpaRepository
                        .findAllByEventBrawlStarsIdAndTierRangeAndPeriodTypeInAndBattleDateBetween(
                            eventBrawlStarsId = eventId.value,
                            tierRange = tierRange,
                            periodTypes = allPeriodTypes,
                            startDate = startDate,
                            endDate = endDate,
                        )
                brawlerId != null ->
                    enemyBattleResultJpaRepository
                        .findAllByBrawlerBrawlStarsIdAndTierRangeAndPeriodTypeInAndBattleDateBetween(
                            brawlerBrawlStarsId = brawlerId.value,
                            tierRange = tierRange,
                            periodTypes = allPeriodTypes,
                            startDate = startDate,
                            endDate = endDate,
                        )
                else ->
                    enemyBattleResultJpaRepository
                        .findAllByTierRangeAndPeriodTypeInAndBattleDateBetween(
                            tierRange = tierRange,
                            periodTypes = allPeriodTypes,
                            startDate = startDate,
                            endDate = endDate,
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
        endDate: LocalDate,
        teamSize: StatsTeamSize,
    ): List<BrawlerTeamResultCount> {
        val entities =
            teamBattleResultJpaRepository.findAllByConditions(
                eventBrawlStarsId = eventId?.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                brawlerBrawlStarsId = brawlerId?.value,
                teamSize = teamSize,
            )

        return entities
            .groupBy { it.brawlerBrawlStarsIds }
            .map { (brawlerBrawlStarsIds, grouped) ->
                BrawlerTeamResultCount(
                    brawlerIds = brawlerBrawlStarsIds.map { BrawlStarsId(it) },
                    resultCount =
                        ResultCount(
                            grouped.sumOf { it.resultCount.victoryCount },
                            grouped.sumOf { it.resultCount.defeatCount },
                            grouped.sumOf { it.resultCount.drawCount },
                        ),
                )
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
}
