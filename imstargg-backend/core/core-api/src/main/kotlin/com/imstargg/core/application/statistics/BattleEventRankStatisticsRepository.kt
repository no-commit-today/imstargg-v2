package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlersRankCount
import com.imstargg.core.domain.statistics.RankCount
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.StatsWeeklyBrawlerDuoBattleRankEntity
import com.imstargg.infrastructure.db.core.statistics.StatsWeeklyBrawlerDuoBattleRankJpaRepository
import com.imstargg.infrastructure.db.core.statistics.StatsWeeklyBrawlerSoloBattleRankJpaRepository
import com.imstargg.infrastructure.db.core.statistics.StatsWeeklyBrawlerTrioBattleRankEntity
import com.imstargg.infrastructure.db.core.statistics.StatsWeeklyBrawlerTrioBattleRankJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleEventRankStatisticsRepository(
    private val soloBattleRankJpaRepository: StatsWeeklyBrawlerSoloBattleRankJpaRepository,
    private val duoBattleRankJpaRepository: StatsWeeklyBrawlerDuoBattleRankJpaRepository,
    private val trioBattleRankJpaRepository: StatsWeeklyBrawlerTrioBattleRankJpaRepository,
) {
    fun findBrawlerRankCounts(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlerRankCount> {
        val entities =
            soloBattleRankJpaRepository
                .findByEventBrawlStarsIdAndTierRangeAndStartDate(eventId.value, tierRange, date)

        return entities.map {
            BrawlerRankCount(
                brawlerId = BrawlStarsId(it.brawlerBrawlStarsId),
                rankCount =
                    RankCount(
                        mapOf(
                            1 to it.rankCount1,
                            2 to it.rankCount2,
                            3 to it.rankCount3,
                            4 to it.rankCount4,
                            5 to it.rankCount5,
                            6 to it.rankCount6,
                            7 to it.rankCount7,
                            8 to it.rankCount8,
                            9 to it.rankCount9,
                            10 to it.rankCount10,
                        ),
                    ),
            )
        }
    }

    fun findBrawlerDuoRankCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlersRankCount> {
        val entities =
            duoBattleRankJpaRepository
                .findByEventAndTierRangeAndStartDateAndBrawlerBrawlStarsId(
                    eventId.value,
                    tierRange,
                    date,
                    brawlerId.value,
                )

        return entities.map { entity ->
            val otherBrawlerId =
                if (entity.brawlerBrawlStarsId1 == brawlerId.value) {
                    BrawlStarsId(entity.brawlerBrawlStarsId2)
                } else {
                    BrawlStarsId(entity.brawlerBrawlStarsId1)
                }

            BrawlersRankCount(
                brawlerIds = listOf(brawlerId, otherBrawlerId),
                rankCount = toDuoRankCount(entity),
            )
        }
    }

    fun findBrawlerTrioRankCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlersRankCount> {
        val entities =
            trioBattleRankJpaRepository
                .findByEventAndTierRangeAndStartDateAndBrawlerBrawlStarsId(
                    eventId.value,
                    tierRange,
                    date,
                    brawlerId.value,
                )

        return entities.map {
            BrawlersRankCount(
                brawlerIds =
                    listOf(
                        BrawlStarsId(it.brawlerBrawlStarsId1),
                        BrawlStarsId(it.brawlerBrawlStarsId2),
                        BrawlStarsId(it.brawlerBrawlStarsId3),
                    ),
                rankCount = toTrioRankCount(it),
            )
        }
    }

    private fun toDuoRankCount(entity: StatsWeeklyBrawlerDuoBattleRankEntity) =
        RankCount(
            mapOf(
                1 to entity.rankCount1,
                2 to entity.rankCount2,
                3 to entity.rankCount3,
                4 to entity.rankCount4,
                5 to entity.rankCount5,
            ),
        )

    private fun toTrioRankCount(entity: StatsWeeklyBrawlerTrioBattleRankEntity) =
        RankCount(
            mapOf(
                1 to entity.rankCount1,
                2 to entity.rankCount2,
                3 to entity.rankCount3,
                4 to entity.rankCount4,
            ),
        )
}
