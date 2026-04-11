package com.imstargg.core.application.statistics

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlerTeamRankStatistics
import com.imstargg.core.domain.statistics.RankCount
import com.imstargg.core.enums.RankStatsSortField
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
                    rankCount = RankCount(grouped.map { it.rankCounts }.reduce { acc, r -> acc + r }.toMap()),
                )
            }
    }

    fun findBrawlerTeamRankCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        teamSize: StatsTeamSize,
        sortField: RankStatsSortField,
        sortOrder: SortOrder,
        offsetLimit: OffsetLimit,
    ): Slice<BrawlerTeamRankStatistics> {
        val tuples =
            teamBattleRankJpaRepository.findPagedByConditions(
                eventBrawlStarsId = eventId.value,
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
                val totalCount = tuple.get(1, Long::class.javaObjectType)!!
                val weightedRankSum = tuple.get(2, Long::class.javaObjectType)!!
                BrawlerTeamRankStatistics(
                    brawlerIds =
                        tuple
                            .get(0, String::class.java)!!
                            .split("_")
                            .map { BrawlStarsId(it.toLong()) },
                    totalBattleCount = totalCount,
                    averageRank = if (totalCount > 0) weightedRankSum.toDouble() / totalCount else 0.0,
                )
            }

        return Slice(content = content, hasNext = hasNext)
    }

    fun countTeamRankTotalBattleCount(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
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
            brawlerBrawlStarsId = brawlerId.value,
            teamSize = teamSize,
        )
}
