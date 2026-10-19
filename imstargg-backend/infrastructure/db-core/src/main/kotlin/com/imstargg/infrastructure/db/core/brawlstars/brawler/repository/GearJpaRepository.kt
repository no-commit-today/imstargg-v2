package com.imstargg.infrastructure.db.core.brawlstars.brawler.repository

import com.imstargg.infrastructure.db.core.brawlstars.brawler.GearEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GearJpaRepository : JpaRepository<GearEntity, Long> {
    fun findAllByDeletedAtIsNull(): List<GearEntity>
}
