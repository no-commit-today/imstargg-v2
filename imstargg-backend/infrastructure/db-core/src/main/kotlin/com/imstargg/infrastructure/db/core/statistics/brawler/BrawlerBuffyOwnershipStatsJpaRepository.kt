package com.imstargg.infrastructure.db.core.statistics.brawler

import com.imstargg.core.enums.BrawlerTrophyRange
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerBuffyOwnershipStatsJpaRepository : JpaRepository<BrawlerBuffyOwnershipStatsEntity, Long> {
    fun findAllByBrawlerBrawlStarsIdAndTrophyRange(
        brawlerBrawlStarsId: Long,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerBuffyOwnershipStatsEntity>
}
