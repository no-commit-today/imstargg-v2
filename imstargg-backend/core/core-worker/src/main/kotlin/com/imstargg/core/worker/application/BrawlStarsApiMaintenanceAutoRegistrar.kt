package com.imstargg.core.worker.application

import com.imstargg.infrastructure.db.core.brawlstars.system.BrawlStarsApiMaintenanceEntity
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiMaintenanceJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

@Component
class BrawlStarsApiMaintenanceAutoRegistrar(
    private val brawlStarsApiMaintenanceJpaRepository: BrawlStarsApiMaintenanceJpaRepository,
    private val clock: Clock,
) {
    companion object {
        private const val DEFAULT_DURATION_HOURS = 1L
        private val log = LoggerFactory.getLogger(BrawlStarsApiMaintenanceAutoRegistrar::class.java)
    }

    fun register() {
        val now = clock.instant()
        if (brawlStarsApiMaintenanceJpaRepository.existsByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now)) {
            return
        }
        val endTime = now.plus(Duration.ofHours(DEFAULT_DURATION_HOURS))
        brawlStarsApiMaintenanceJpaRepository.save(
            BrawlStarsApiMaintenanceEntity(
                startTime = now,
                endTime = endTime,
            ),
        )
        log.warn("[BrawlStars API 점검] 자동 점검 등록 startTime={} endTime={}", now, endTime)
    }
}
