package com.imstargg.core.application.statistics

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlerTeamRankStatisticsItem
import com.imstargg.core.enums.RankStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleRankStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerTeamBattleRankStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleRankStatisticsRepository(
    private val soloBattleRankJpaRepository: BrawlerBattleRankStatsJpaRepository,
    private val teamBattleRankJpaRepository: BrawlerTeamBattleRankStatsJpaRepository,
) {
    fun findBrawlerRankCounts(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerRankCount> {
        val entities =
            soloBattleRankJpaRepository
                .findAllByEventBrawlStarsIdAndTierRangeAndBattleDateBetween(eventId.value, tierRange, startDate, endDate)

        return entities
            .groupBy { it.brawlerBrawlStarsId }
            .map { (brawlerBrawlStarsId, grouped) ->
                BrawlerRankCount(
                    brawlerId = BrawlStarsId(brawlerBrawlStarsId),
                    rankCount = grouped.map { it.rankCounts }.reduce { acc, r -> acc + r }.toRankCount(),
                )
            }
    }

    fun findBrawlerTeamRankCounts(
        eventId: BrawlStarsId,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
        sortField: RankStatsSortBy,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): List<BrawlerTeamRankStatisticsItem> =
        teamBattleRankJpaRepository
            .findPagedByConditions(
                eventBrawlStarsId = eventId.value,
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
                BrawlerTeamRankStatisticsItem(
                    brawlerIds = projection.brawlerTeam.brawlerBrawlStarsIds.map { BrawlStarsId(it) },
                    totalBattleCount = projection.totalCount,
                    averageRank =
                        if (projection.totalCount > 0) {
                            projection.weightedRankSum.toDouble() / projection.totalCount
                        } else {
                            0.0
                        },
                )
            }

    fun countTeamRankTotalBattleCount(
        eventId: BrawlStarsId,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
    ): Long =
        teamBattleRankJpaRepository.countTotalBattleCount(
            eventBrawlStarsId = eventId.value,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            brawlerBrawlStarsIds = brawlerIds.map { it.value },
            teamSize = teamSize,
        )

    fun countTeamRankBy(
        eventId: BrawlStarsId,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
    ): Long =
        teamBattleRankJpaRepository.countDistinctTeamKeys(
            eventBrawlStarsId = eventId.value,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
            brawlerBrawlStarsIds = brawlerIds.map { it.value },
            teamSize = teamSize,
        )
}
