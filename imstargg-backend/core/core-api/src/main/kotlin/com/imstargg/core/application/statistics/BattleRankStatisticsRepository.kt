package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerPairRankStatistics
import com.imstargg.core.domain.statistics.BrawlerPairRankStatisticsItem
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlerRankStatistics
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
    fun findBrawlerRankStatistics(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerRankStatistics {
        val items =
            soloBattleRankJpaRepository
                .findAllByEventBrawlStarsIdAndTierRangeAndBattleDateBetween(
                    eventId.value,
                    tierRange,
                    startDate,
                    endDate,
                ).groupBy { it.brawlerBrawlStarsId }
                .map { (brawlerBrawlStarsId, grouped) ->
                    BrawlerRankCount(
                        brawlerId = BrawlStarsId(brawlerBrawlStarsId),
                        rankCount = grouped.map { it.rankCounts }.reduce { acc, r -> acc + r }.toRankCount(),
                    )
                }.map { it.toStatisticsItem() }
        return BrawlerRankStatistics(items = items)
    }

    fun findBrawlerPairRankStatistics(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerPairRankStatistics {
        val items: List<BrawlerPairRankStatisticsItem> =
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
        val totalBattleCount =
            pairBattleRankJpaRepository.countTotalBattleCount(
                eventBrawlStarsId = eventId.value,
                brawlerBrawlStarsId = brawlerId.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val totalElementCount =
            pairBattleRankJpaRepository.countDistinctPairBrawlers(
                eventBrawlStarsId = eventId.value,
                brawlerBrawlStarsId = brawlerId.value,
                pairBrawlerBrawlStarsId = pairBrawlerId?.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        return BrawlerPairRankStatistics(
            totalBattleCount = totalBattleCount,
            totalElementCount = totalElementCount,
            items = items,
        )
    }
}
