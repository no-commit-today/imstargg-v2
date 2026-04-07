package com.imstargg.infrastructure.db.core.brawlstars.brawler.repository

import com.imstargg.infrastructure.db.core.brawlstars.brawler.StarPowerEntity
import org.springframework.data.jpa.repository.JpaRepository

interface StarPowerJpaRepository : JpaRepository<StarPowerEntity, Long> {
    fun findAllByBrawlerBrawlStarsId(brawlerBrawlStarsId: Long): List<StarPowerEntity>
}
