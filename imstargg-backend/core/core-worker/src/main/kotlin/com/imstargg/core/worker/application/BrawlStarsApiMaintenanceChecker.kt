package com.imstargg.core.worker.application

import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsApiMaintenanceJpaRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.OffsetDateTime

@Component
class BrawlStarsApiMaintenanceChecker(
    private val brawlStarsApiMaintenanceJpaRepository: BrawlStarsApiMaintenanceJpaRepository,
    private val clock: Clock,
) {
    fun isUnderMaintenance(): Boolean {
        val now = OffsetDateTime.now(clock)
        return brawlStarsApiMaintenanceJpaRepository
            .existsByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now)
    }
}
