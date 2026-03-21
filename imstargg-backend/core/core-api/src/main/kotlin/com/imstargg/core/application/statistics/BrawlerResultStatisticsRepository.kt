package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
import com.imstargg.core.domain.statistics.BrawlersResultCount
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.domain.statistics.StarPlayerCount
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultWeeklyStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BrawlerResultStatisticsRepository(
    private val battleResultJpaRepository: BrawlerBattleResultStatsJpaRepository,
    private val teamBattleResultJpaRepository: BrawlerTeamBattleResultWeeklyStatsJpaRepository,
) {
    fun findBrawlerResultCounts(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultCount> {
        val aggregates = battleResultJpaRepository.aggregateByBrawler(tierRange, startDate, endDate)

        return aggregates.map {
            BrawlerResultCount(
                brawlerId = BrawlStarsId(it.brawlerBrawlStarsId),
                resultCount = ResultCount(it.victoryCount, it.defeatCount, it.drawCount),
                starPlayerCount = StarPlayerCount(it.starPlayerCount),
            )
        }
    }

    fun findBrawlerDailyResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultDailyCount> {
        val aggregates = battleResultJpaRepository.aggregateByDate(brawlerId.value, tierRange, startDate, endDate)

        return aggregates
            .map {
                BrawlerResultDailyCount(
                    date = it.date,
                    resultCount = ResultCount(it.victoryCount, it.defeatCount, it.drawCount),
                    starPlayerCount = StarPlayerCount(it.starPlayerCount),
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
                .findAllByTierRangeAndDateAndBrawlerBrawlStarsId(tierRange, startDate, brawlerId.value)

        return BattleEventResultStatisticsRepository.extractDuoResultCounts(entities, brawlerId)
    }

    fun findBrawlerTrioResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
    ): List<BrawlersResultCount> {
        val entities =
            teamBattleResultJpaRepository
                .findAllByTierRangeAndDateAndBrawlerBrawlStarsId(tierRange, startDate, brawlerId.value)

        return entities.map {
            BrawlersResultCount(
                brawlerIds = it.teamBrawlerBrawlStarsIds.map { id -> BrawlStarsId(id) },
                resultCount = ResultCount(it.victoryCount, it.defeatCount, it.drawCount),
            )
        }
    }
}
