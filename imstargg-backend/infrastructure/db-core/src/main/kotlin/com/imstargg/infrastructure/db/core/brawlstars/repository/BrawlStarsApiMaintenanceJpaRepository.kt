package com.imstargg.infrastructure.db.core.brawlstars.repository

import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsApiMaintenanceEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface BrawlStarsApiMaintenanceJpaRepository : JpaRepository<BrawlStarsApiMaintenanceEntity, Long> {
    fun existsByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
        startTime: OffsetDateTime,
        endTime: OffsetDateTime,
    ): Boolean
}
