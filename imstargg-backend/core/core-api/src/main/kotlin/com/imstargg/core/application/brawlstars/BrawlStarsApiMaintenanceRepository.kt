package com.imstargg.core.application.brawlstars

import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiMaintenanceJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Repository
@Transactional(readOnly = true)
class BrawlStarsApiMaintenanceRepository(
    private val brawlStarsApiMaintenanceJpaRepository: BrawlStarsApiMaintenanceJpaRepository,
    private val clock: Clock,
) {
    fun isUnderMaintenance(): Boolean {
        val now = clock.instant()
        return brawlStarsApiMaintenanceJpaRepository
            .existsByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now)
    }
}
