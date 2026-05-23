package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerPairRankStatisticsItem
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.core.infrastructure.jpa.toRankCount
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleRankStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerPairBattleRankStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleRankStatisticsRepository(
    private val soloBattleRankJpaRepository: BrawlerBattleRankStatsJpaRepository,
    private val pairBattleRankJpaRepository: BrawlerPairBattleRankStatsJpaRepository,
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

    fun findBrawlerPairRankCounts(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerPairRankStatisticsItem> =
        pairBattleRankJpaRepository
            .findAllByConditions(
                eventBrawlStarsId = eventId.value,
                brawlerBrawlStarsId = brawlerId.value,
                pairBrawlerBrawlStarsId = pairBrawlerId?.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            ).map { projection ->
                BrawlerPairRankStatisticsItem(
                    brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                    pairBrawlerId = BrawlStarsId(projection.pairBrawlerBrawlStarsId),
                    totalBattleCount = projection.totalCount,
                    averageRank =
                        if (projection.totalCount > 0) {
                            projection.weightedRankSum.toDouble() / projection.totalCount
                        } else {
                            0.0
                        },
                )
            }

    fun countPairRankTotalBattleCount(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        pairBattleRankJpaRepository.countTotalBattleCount(
            eventBrawlStarsId = eventId.value,
            brawlerBrawlStarsId = brawlerId.value,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )

    fun countPairRankBy(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        pairBattleRankJpaRepository.countDistinctPairBrawlers(
            eventBrawlStarsId = eventId.value,
            brawlerBrawlStarsId = brawlerId.value,
            pairBrawlerBrawlStarsId = pairBrawlerId?.value,
            tierRange = tierRange,
            startDate = startDate,
            endDate = endDate,
        )
}
