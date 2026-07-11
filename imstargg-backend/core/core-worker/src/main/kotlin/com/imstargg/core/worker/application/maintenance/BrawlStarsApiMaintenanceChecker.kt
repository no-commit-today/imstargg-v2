package com.imstargg.core.worker.application.maintenance

import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiMaintenanceJpaRepository
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class BrawlStarsApiMaintenanceChecker(
    private val brawlStarsApiMaintenanceJpaRepository: BrawlStarsApiMaintenanceJpaRepository,
    private val clock: Clock,
) {
    fun isUnderMaintenance(): Boolean {
        val now = clock.instant()
        return brawlStarsApiMaintenanceJpaRepository
            .existsByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now)
    }
}
