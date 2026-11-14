package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerRankCount
import com.imstargg.core.domain.statistics.BrawlerRankStatistics
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.core.infrastructure.jpa.toRankCount
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleRankStatsBackupJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleRankStatisticsRepository(
    private val soloBattleRankJpaRepository: BrawlerBattleRankStatsBackupJpaRepository,
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
}
