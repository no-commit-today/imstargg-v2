package com.imstargg.infrastructure.db.core.brawlstars.brawler.repository

import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BrawlerJpaRepository : JpaRepository<BrawlerEntity, Long> {
    fun findByBrawlStarsId(brawlStarsId: Long): BrawlerEntity?
}
