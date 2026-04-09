package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlerTeamRankCount
import com.imstargg.core.domain.statistics.RankCount
import com.imstargg.core.enums.StatsPeriodType
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
        teamSize: StatsTeamSize,
    ): List<BrawlerTeamRankCount> {
        val entities =
            teamBattleRankJpaRepository
                .findAllByEventAndConditionsAndBrawlerAndTeamSize(
                    eventId.value,
                    StatsPeriodType.WEEKLY,
                    tierRange,
                    startDate,
                    brawlerId.value,
                    teamSize,
                )

        return entities.map { entity ->
            BrawlerTeamRankCount(
                brawlerIds = entity.brawlerBrawlStarsIds.map { BrawlStarsId(it) },
                rankCount = RankCount(entity.rankCounts.toMap()),
            )
        }
    }
}
