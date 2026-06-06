package com.imstargg.infrastructure.db.core.brawlstars.system.repository

import com.imstargg.infrastructure.db.core.brawlstars.system.BrawlStarsApiMaintenanceEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface BrawlStarsApiMaintenanceJpaRepository : JpaRepository<BrawlStarsApiMaintenanceEntity, Long> {
    fun existsByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
        startTime: Instant,
        endTime: Instant,
    ): Boolean
}
