package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlersRankCount
import com.imstargg.core.domain.statistics.RankCount
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerSoloBattleRankStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerTeamBattleRankStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleEventRankStatisticsRepository(
    private val soloBattleRankJpaRepository: BrawlerSoloBattleRankStatsJpaRepository,
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
                .findByEventBrawlStarsIdAndTierRangeAndDateBetween(eventId.value, tierRange, startDate, endDate)

        return entities
            .groupBy { it.brawlerBrawlStarsId }
            .map { (brawlerBrawlStarsId, grouped) ->
                BrawlerRankCount(
                    brawlerId = BrawlStarsId(brawlerBrawlStarsId),
                    rankCount = RankCount(grouped.map { it.rankCounts }.reduce { acc, r -> acc + r }.toMap()),
                )
            }
    }

    fun findBrawlerDuoRankCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlersRankCount> {
        val entities =
            teamBattleRankJpaRepository
                .findAllByEventBrawlStarsIdAndPeriodTypeAndTierRangeAndDateAndBrawlerBrawlStarsId(
                    eventId.value,
                    StatsPeriodType.WEEKLY,
                    tierRange,
                    startDate,
                    brawlerId.value,
                ).filter { it.teammateBrawlStarsIds.size == 1 }

        return entities.map { entity ->
            BrawlersRankCount(
                brawlerIds = entity.teamBrawlerBrawlStarsIds.map { BrawlStarsId(it) },
                rankCount = RankCount(entity.rankCounts.toMap()),
            )
        }
    }

    fun findBrawlerTrioRankCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlersRankCount> {
        val entities =
            teamBattleRankJpaRepository
                .findAllByEventBrawlStarsIdAndPeriodTypeAndTierRangeAndDateAndBrawlerBrawlStarsId(
                    eventId.value,
                    StatsPeriodType.WEEKLY,
                    tierRange,
                    startDate,
                    brawlerId.value,
                ).filter { it.teammateBrawlStarsIds.size == 2 }

        return entities.map { entity ->
            BrawlersRankCount(
                brawlerIds = entity.teamBrawlerBrawlStarsIds.map { BrawlStarsId(it) },
                rankCount = RankCount(entity.rankCounts.toMap()),
            )
        }
    }
}
