package com.imstargg.core.application.statistics

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
import com.imstargg.core.domain.statistics.BrawlerTeamResultCount
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.enums.ResultStatsSortField
import com.imstargg.core.enums.SortOrder
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
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        sortField: ResultStatsSortField,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): Slice<BrawlerEnemyResultCount> {
        val tuples =
            enemyBattleResultJpaRepository.findPagedByConditions(
                eventBrawlStarsId = eventId?.value,
                brawlerBrawlStarsId = brawlerId.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                sortField = sortField,
                sortOrder = sortOrder,
                offset = offsetLimit.offset.toLong(),
                limit = (offsetLimit.limit + 1).toLong(),
            )

        val hasNext = tuples.size > offsetLimit.limit
        val content =
            tuples.take(offsetLimit.limit).map { tuple ->
                BrawlerEnemyResultCount(
                    brawlerId = BrawlStarsId(tuple.get(0, Long::class.javaObjectType)!!),
                    enemyBrawlerId = BrawlStarsId(tuple.get(1, Long::class.javaObjectType)!!),
                    resultCount =
                        ResultCount(
                            victoryCount = tuple.get(2, Long::class.javaObjectType)!!,
                            defeatCount = tuple.get(3, Long::class.javaObjectType)!!,
                            drawCount = tuple.get(4, Long::class.javaObjectType)!!,
                        ),
                )
            }

        return Slice(content = content, hasNext = hasNext)
    }

    fun countEnemyResultTotalBattleCount(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        enemyBattleResultJpaRepository.countTotalBattleCount(
            eventBrawlStarsId = eventId?.value,
            brawlerBrawlStarsId = brawlerId.value,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )

    fun findBrawlerTeamResultCounts(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
        sortField: ResultStatsSortField,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): Slice<BrawlerTeamResultCount> {
        val tuples =
            teamBattleResultJpaRepository.findPagedByConditions(
                eventBrawlStarsId = eventId?.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                brawlerBrawlStarsId = brawlerId.value,
                teamSize = teamSize,
                sortField = sortField,
                sortOrder = sortOrder,
                offset = offsetLimit.offset.toLong(),
                limit = (offsetLimit.limit + 1).toLong(),
            )

        val hasNext = tuples.size > offsetLimit.limit
        val content =
            tuples.take(offsetLimit.limit).map { tuple ->
                BrawlerTeamResultCount(
                    brawlerIds =
                        tuple
                            .get(0, String::class.java)!!
                            .split("_")
                            .map { BrawlStarsId(it.toLong()) },
                    resultCount =
                        ResultCount(
                            victoryCount = tuple.get(1, Long::class.javaObjectType)!!,
                            defeatCount = tuple.get(2, Long::class.javaObjectType)!!,
                            drawCount = tuple.get(3, Long::class.javaObjectType)!!,
                        ),
                )
            }

        return Slice(content = content, hasNext = hasNext)
    }

    fun countTeamResultTotalBattleCount(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
    ): Long =
        teamBattleResultJpaRepository.countTotalBattleCount(
            eventBrawlStarsId = eventId?.value,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            brawlerBrawlStarsId = brawlerId.value,
            teamSize = teamSize,
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
