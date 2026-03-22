package com.imstargg.infrastructure.db.core.brawlstars.brawler.repository

import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerGearEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerGearJpaRepository : JpaRepository<BrawlerGearEntity, Long> {
    fun findAllByBrawlerBrawlStarsId(brawlerBrawlStarsId: Long): List<BrawlerGearEntity>

    fun deleteAllByBrawlerBrawlStarsId(brawlerBrawlStarsId: Long)
}
