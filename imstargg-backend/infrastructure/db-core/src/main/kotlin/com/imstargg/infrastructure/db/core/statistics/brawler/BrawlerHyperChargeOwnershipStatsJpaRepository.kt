package com.imstargg.infrastructure.db.core.statistics.brawler

import com.imstargg.core.enums.BrawlerTrophyRange
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerHyperChargeOwnershipStatsJpaRepository : JpaRepository<BrawlerHyperChargeOwnershipStatsEntity, Long> {
    fun findAllByBrawlerBrawlStarsIdAndTrophyRange(
        brawlerBrawlStarsId: Long,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerHyperChargeOwnershipStatsEntity>
}
