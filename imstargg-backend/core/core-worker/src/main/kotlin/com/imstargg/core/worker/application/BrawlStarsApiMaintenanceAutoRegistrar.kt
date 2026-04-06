package com.imstargg.core.worker.application

import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsApiMaintenanceEntity
import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsApiMaintenanceJpaRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.OffsetDateTime

@Component
class BrawlStarsApiMaintenanceAutoRegistrar(
    private val brawlStarsApiMaintenanceJpaRepository: BrawlStarsApiMaintenanceJpaRepository,
    private val clock: Clock,
) {
    companion object {
        private const val DEFAULT_DURATION_HOURS = 1L
    }

    fun register() {
        val now = OffsetDateTime.now(clock)
        if (brawlStarsApiMaintenanceJpaRepository.existsByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now)) {
            return
        }
        brawlStarsApiMaintenanceJpaRepository.save(
            BrawlStarsApiMaintenanceEntity(
                startTime = now,
                endTime = now.plusHours(DEFAULT_DURATION_HOURS),
            ),
        )
    }
}
