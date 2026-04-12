package com.imstargg.infrastructure.db.core.statistics.brawler

import com.imstargg.core.enums.BrawlerTrophyRange
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerGearOwnershipStatsJpaRepository : JpaRepository<BrawlerGearOwnershipStatsEntity, Long> {
    fun findAllByBrawlerBrawlStarsIdAndTrophyRange(
        brawlerBrawlStarsId: Long,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerGearOwnershipStatsEntity>
}
