package com.imstargg.infrastructure.db.core.brawlstars.brawler.repository

import com.imstargg.infrastructure.db.core.brawlstars.brawler.HyperChargeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface HyperChargeJpaRepository : JpaRepository<HyperChargeEntity, Long> {
    fun findAllByBrawlerBrawlStarsId(brawlerBrawlStarsId: Long): List<HyperChargeEntity>
}
