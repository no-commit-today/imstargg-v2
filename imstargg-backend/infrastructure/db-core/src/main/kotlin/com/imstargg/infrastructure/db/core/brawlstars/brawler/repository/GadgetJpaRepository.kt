package com.imstargg.infrastructure.db.core.brawlstars.brawler.repository

import com.imstargg.infrastructure.db.core.brawlstars.brawler.GadgetEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GadgetJpaRepository : JpaRepository<GadgetEntity, Long>
