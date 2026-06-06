package com.imstargg.infrastructure.db.core.brawlstars.system.repository

import com.imstargg.infrastructure.db.core.brawlstars.system.BrawlStarsApiSpecEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface BrawlStarsApiSpecJpaRepository : JpaRepository<BrawlStarsApiSpecEntity, Long> {
    fun findTopByOrderByCollectedAtDesc(): BrawlStarsApiSpecEntity?

    fun findTopByCollectedAtLessThanOrderByCollectedAtDesc(collectedAt: Instant): BrawlStarsApiSpecEntity?

    fun findAllByOrderByCollectedAtDesc(): List<BrawlStarsApiSpecEntity>
}
