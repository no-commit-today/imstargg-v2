package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlersRankCount
import com.imstargg.core.domain.statistics.RankCount
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerDuoBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerDuoBattleRankStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.BrawlerSoloBattleRankStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.BrawlerTrioBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTrioBattleRankStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleEventRankStatisticsRepository(
    private val soloBattleRankJpaRepository: BrawlerSoloBattleRankStatsJpaRepository,
    private val duoBattleRankJpaRepository: BrawlerDuoBattleRankStatsJpaRepository,
    private val trioBattleRankJpaRepository: BrawlerTrioBattleRankStatsJpaRepository,
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
                    rankCount =
                        RankCount(
                            mapOf(
                                1 to grouped.sumOf { it.rankCount1 },
                                2 to grouped.sumOf { it.rankCount2 },
                                3 to grouped.sumOf { it.rankCount3 },
                                4 to grouped.sumOf { it.rankCount4 },
                                5 to grouped.sumOf { it.rankCount5 },
                                6 to grouped.sumOf { it.rankCount6 },
                                7 to grouped.sumOf { it.rankCount7 },
                                8 to grouped.sumOf { it.rankCount8 },
                                9 to grouped.sumOf { it.rankCount9 },
                                10 to grouped.sumOf { it.rankCount10 },
                            ),
                        ),
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
            duoBattleRankJpaRepository
                .findAllByEventBrawlStarsIdAndPeriodTypeAndTierRangeAndDateAndBrawlerBrawlStarsId(
                    eventId.value,
                    StatsPeriodType.WEEKLY,
                    tierRange,
                    startDate,
                    brawlerId.value,
                )

        return entities.map { entity ->
            BrawlersRankCount(
                brawlerIds = listOf(brawlerId, BrawlStarsId(entity.teammateBrawlStarsId)),
                rankCount = toDuoRankCount(entity),
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
            trioBattleRankJpaRepository
                .findAllByEventBrawlStarsIdAndPeriodTypeAndTierRangeAndDateAndBrawlerBrawlStarsId(
                    eventId.value,
                    StatsPeriodType.WEEKLY,
                    tierRange,
                    startDate,
                    brawlerId.value,
                )

        return entities.map {
            BrawlersRankCount(
                brawlerIds = it.teamBrawlerBrawlStarsIds.map { id -> BrawlStarsId(id) },
                rankCount = toTrioRankCount(it),
            )
        }
    }

    private fun toDuoRankCount(entity: BrawlerDuoBattleRankStatsEntity) =
        RankCount(
            mapOf(
                1 to entity.rankCount1,
                2 to entity.rankCount2,
                3 to entity.rankCount3,
                4 to entity.rankCount4,
                5 to entity.rankCount5,
            ),
        )

    private fun toTrioRankCount(entity: BrawlerTrioBattleRankStatsEntity) =
        RankCount(
            mapOf(
                1 to entity.rankCount1,
                2 to entity.rankCount2,
                3 to entity.rankCount3,
                4 to entity.rankCount4,
            ),
        )
}
