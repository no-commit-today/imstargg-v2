package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlersResultCount
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.domain.statistics.StarPlayerCount
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.StatsWeeklyBrawlerBattleResultJpaRepository
import com.imstargg.infrastructure.db.core.statistics.StatsWeeklyBrawlerTeamBattleResultJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BrawlerResultStatisticsRepository(
    private val battleResultJpaRepository: StatsWeeklyBrawlerBattleResultJpaRepository,
    private val teamBattleResultJpaRepository: StatsWeeklyBrawlerTeamBattleResultJpaRepository,
) {
    fun findBrawlerResultCounts(
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlerResultCount> {
        val aggregates = battleResultJpaRepository.aggregateByBrawler(tierRange, date)

        return aggregates.map {
            BrawlerResultCount(
                brawlerId = BrawlStarsId(it.brawlerBrawlStarsId),
                resultCount = ResultCount(it.victoryCount, it.defeatCount, it.drawCount),
                starPlayerCount = StarPlayerCount(it.starPlayerCount),
            )
        }
    }

    fun findBrawlerDuoResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlersResultCount> {
        val entities =
            teamBattleResultJpaRepository
                .findByTierRangeAndStartDateAndBrawlerBrawlStarsId(tierRange, date, brawlerId.value)

        return BattleEventResultStatisticsRepository.extractDuoResultCounts(entities, brawlerId)
    }

    fun findBrawlerTrioResultCounts(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        date: LocalDate,
    ): List<BrawlersResultCount> {
        val entities =
            teamBattleResultJpaRepository
                .findByTierRangeAndStartDateAndBrawlerBrawlStarsId(tierRange, date, brawlerId.value)

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
}
