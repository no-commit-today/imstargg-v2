package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
import com.imstargg.core.domain.statistics.BrawlersResultCount
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerTeamBattleResultStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BrawlerResultStatisticsRepository(
    private val battleResultJpaRepository: BrawlerBattleResultStatsJpaRepository,
    private val teamBattleResultJpaRepository: BrawlerTeamBattleResultStatsJpaRepository,
) {
    fun findBrawlerResultCounts(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultCount> {
        val entities = battleResultJpaRepository.findAllByTierRangeAndBattleDateBetween(tierRange, startDate, endDate)

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

    fun findBrawlerDailyResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultDailyCount> {
        val entities =
            battleResultJpaRepository
                .findAllByBrawlerBrawlStarsIdAndTierRangeAndBattleDateBetween(
                    brawlerId.value,
                    tierRange,
                    startDate,
                    endDate,
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

    fun findBrawlerDuoResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlersResultCount> {
        val entities =
            teamBattleResultJpaRepository
                .findAllByConditionsAndBrawler(
                    StatsPeriodType.WEEKLY,
                    tierRange,
                    startDate,
                    brawlerId.value,
                )

        return BattleEventResultStatisticsRepository.extractDuoResultCounts(entities, brawlerId)
    }

    fun findBrawlerTrioResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlersResultCount> {
        val entities =
            teamBattleResultJpaRepository
                .findAllByConditionsAndBrawler(
                    StatsPeriodType.WEEKLY,
                    tierRange,
                    startDate,
                    brawlerId.value,
                )

        return entities.map {
            BrawlersResultCount(
                brawlerIds = it.brawlerBrawlStarsIds.map { id -> BrawlStarsId(id) },
                resultCount = ResultCount(it.resultCount.victoryCount, it.resultCount.defeatCount, it.resultCount.drawCount),
            )
        }
    }
}
