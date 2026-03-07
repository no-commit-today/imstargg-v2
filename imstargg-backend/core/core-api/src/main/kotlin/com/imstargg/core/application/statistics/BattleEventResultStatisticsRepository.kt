package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlersResultCount
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.domain.statistics.StarPlayerCount
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultWeeklyStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultWeeklyStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultWeeklyStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleEventResultStatisticsRepository(
    private val battleResultJpaRepository: BrawlerBattleResultStatsJpaRepository,
    private val teamBattleResultJpaRepository: BrawlerTeamBattleResultWeeklyStatsJpaRepository,
    private val enemyBattleResultJpaRepository: BrawlerEnemyBattleResultWeeklyStatsJpaRepository,
) {
    fun findBrawlerResultCounts(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultCount> {
        val entities =
            battleResultJpaRepository
                .findByEventBrawlStarsIdAndTierRangeAndDateBetween(eventId.value, tierRange, startDate, endDate)

        return entities
            .groupBy { it.brawlerBrawlStarsId }
            .map { (brawlerBrawlStarsId, grouped) ->
                BrawlerResultCount(
                    brawlerId = BrawlStarsId(brawlerBrawlStarsId),
                    resultCount =
                        ResultCount(
                            grouped.sumOf { it.victoryCount },
                            grouped.sumOf { it.defeatCount },
                            grouped.sumOf { it.drawCount },
                        ),
                    starPlayerCount = StarPlayerCount(grouped.sumOf { it.starPlayerCount }),
                )
            }
    }

    fun findBrawlerDuoResultCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlersResultCount> {
        val entities =
            teamBattleResultJpaRepository
                .findByEventAndTierRangeAndDateAndBrawlerBrawlStarsId(
                    eventId.value,
                    tierRange,
                    startDate,
                    brawlerId.value,
                )

        return extractDuoResultCounts(entities, brawlerId)
    }

    fun findBrawlerTrioResultCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlersResultCount> {
        val entities =
            teamBattleResultJpaRepository
                .findByEventAndTierRangeAndDateAndBrawlerBrawlStarsId(
                    eventId.value,
                    tierRange,
                    startDate,
                    brawlerId.value,
                )

        return entities.map {
            BrawlersResultCount(
                brawlerIds =
                    listOf(
                        BrawlStarsId(it.brawlerBrawlStarsId1),
                        BrawlStarsId(it.brawlerBrawlStarsId2),
                        BrawlStarsId(it.brawlerBrawlStarsId3),
                    ),
                resultCount = ResultCount(it.victoryCount, it.defeatCount, it.drawCount),
            )
        }
    }

    fun findBrawlerEnemyResultCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlerEnemyResultCount> {
        val entities =
            enemyBattleResultJpaRepository
                .findByEventBrawlStarsIdAndBrawlerBrawlStarsIdAndTierRangeAndDate(
                    eventId.value,
                    brawlerId.value,
                    tierRange,
                    startDate,
                )

        return entities.map {
            BrawlerEnemyResultCount(
                brawlerId = BrawlStarsId(it.brawlerBrawlStarsId),
                enemyBrawlerId = BrawlStarsId(it.enemyBrawlerBrawlStarsId),
                resultCount = ResultCount(it.victoryCount, it.defeatCount, it.drawCount),
            )
        }
    }

    companion object {
        fun extractDuoResultCounts(
            entities: List<BrawlerTeamBattleResultWeeklyStatsEntity>,
            brawlerId: BrawlStarsId,
        ): List<BrawlersResultCount> {
            val otherBrawlerIdToResultCount = mutableMapOf<BrawlStarsId, ResultCount>()

            for (entity in entities) {
                val resultCount = ResultCount(entity.victoryCount, entity.defeatCount, entity.drawCount)

                val otherIds =
                    listOf(
                        entity.brawlerBrawlStarsId1,
                        entity.brawlerBrawlStarsId2,
                        entity.brawlerBrawlStarsId3,
                    ).filter { it != brawlerId.value }

                for (otherId in otherIds) {
                    val otherBrawlerId = BrawlStarsId(otherId)
                    otherBrawlerIdToResultCount.merge(otherBrawlerId, resultCount) { existing, new ->
                        existing.merge(new)
                    }
                }
            }

            return otherBrawlerIdToResultCount.map { (otherBrawlerId, resultCount) ->
                BrawlersResultCount(
                    brawlerIds = listOf(brawlerId, otherBrawlerId),
                    resultCount = resultCount,
                )
            }
        }
    }
}
