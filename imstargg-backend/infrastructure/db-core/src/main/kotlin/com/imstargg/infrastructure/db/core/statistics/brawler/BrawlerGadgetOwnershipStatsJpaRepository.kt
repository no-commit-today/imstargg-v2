package com.imstargg.infrastructure.db.core.statistics.brawler

import com.imstargg.core.enums.BrawlerTrophyRange
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerGadgetOwnershipStatsJpaRepository : JpaRepository<BrawlerGadgetOwnershipStatsEntity, Long> {
    fun findAllByBrawlerBrawlStarsIdAndTrophyRange(
        brawlerBrawlStarsId: Long,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerGadgetOwnershipStatsEntity>
}
