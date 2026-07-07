package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerPairRankCount
import com.imstargg.core.domain.statistics.BrawlerPairRankStatistics
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
        val counts =
            soloBattleRankJpaRepository
                .findAllByEventBrawlStarsIdAndTierRangeAndBattleDateBetween(
                    eventId.value,
                    tierRange,
                    startDate,
                    endDate,
                ).map { entity ->
                    BrawlerRankCount(
                        brawlerId = BrawlStarsId(entity.brawlerBrawlStarsId),
                        rankCount = entity.rankCounts.toRankCount(),
                    )
                }
        return BrawlerRankStatistics.create(counts)
    }

    fun findBrawlerPairRankStatistics(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerPairRankStatistics {
        val counts =
            pairBattleRankJpaRepository
                .findAllByConditions(
                    eventBrawlStarsId = eventId.value,
                    brawlerBrawlStarsId = brawlerId.value,
                    pairBrawlerBrawlStarsId = pairBrawlerId?.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerPairRankCount(
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        pairBrawlerId = BrawlStarsId(projection.pairBrawlerBrawlStarsId),
                        totalCount = projection.totalCount,
                        weightedRankSum = projection.weightedRankSum,
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
        return BrawlerPairRankStatistics.create(
            totalBattleCount = totalBattleCount,
            totalElementCount = totalElementCount,
            counts = counts,
        )
    }
}
