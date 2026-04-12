package com.imstargg.core.application.statistics

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
import com.imstargg.core.domain.statistics.BrawlerTeamResultCount
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.enums.ResultStatsSortBy
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
                    resultCount = grouped.map { it.resultCount }.reduce { acc, r -> acc + r }.toResultCount(),
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
        sortField: ResultStatsSortBy,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): List<BrawlerEnemyResultCount> =
        enemyBattleResultJpaRepository
            .findPagedByConditions(
                eventBrawlStarsId = eventId?.value,
                brawlerBrawlStarsId = brawlerId.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                sortField = sortField,
                sortOrder = sortOrder,
                offset = offsetLimit.offset.toLong(),
                limit = offsetLimit.limit.toLong(),
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

    fun findBrawlerTeamResultCounts(
        eventId: BrawlStarsId?,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
        sortField: ResultStatsSortBy,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): List<BrawlerTeamResultCount> =
        teamBattleResultJpaRepository
            .findPagedByConditions(
                eventBrawlStarsId = eventId?.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                brawlerBrawlStarsIds = brawlerIds.map { it.value },
                teamSize = teamSize,
                sortBy = sortField,
                sortOrder = sortOrder,
                offset = offsetLimit.offset.toLong(),
                limit = offsetLimit.limit.toLong(),
            ).map { projection ->
                BrawlerTeamResultCount(
                    brawlerIds = projection.brawlerTeam.brawlerBrawlStarsIds.map { BrawlStarsId(it) },
                    resultCount =
                        ResultCount(
                            victoryCount = projection.victoryCount,
                            defeatCount = projection.defeatCount,
                            drawCount = projection.drawCount,
                        ),
                )
            }

    fun countTeamResultTotalBattleCount(
        eventId: BrawlStarsId?,
        brawlerIds: List<BrawlStarsId>,
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
            brawlerBrawlStarsIds = brawlerIds.map { it.value },
            teamSize = teamSize,
        )

    fun countTeamResultBy(
        eventId: BrawlStarsId?,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
    ): Long =
        teamBattleResultJpaRepository.countDistinctTeamKeys(
            eventBrawlStarsId = eventId?.value,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            brawlerBrawlStarsIds = brawlerIds.map { it.value },
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
                            resultCount = grouped.map { it.resultCount }.reduce { acc, r -> acc + r }.toResultCount(),
                            starPlayerCount = grouped.sumOf { it.starPlayerCount },
                        ),
                )
            }.sortedBy { it.date }
    }
}
