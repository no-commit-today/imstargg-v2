package com.imstargg.core.application.brawlstars

import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsApiMaintenanceJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime

@Repository
@Transactional(readOnly = true)
class BrawlStarsApiMaintenanceRepository(
    private val brawlStarsApiMaintenanceJpaRepository: BrawlStarsApiMaintenanceJpaRepository,
    private val clock: Clock,
) {
    fun isUnderMaintenance(): Boolean {
        val now = OffsetDateTime.now(clock)
        return brawlStarsApiMaintenanceJpaRepository
            .existsByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now)
    }
}
