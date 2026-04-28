package com.imstargg.core.worker.application

import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsApiMaintenanceEntity
import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsApiMaintenanceJpaRepository
import org.slf4j.LoggerFactory
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
        private val log = LoggerFactory.getLogger(BrawlStarsApiMaintenanceAutoRegistrar::class.java)
    }

    fun register() {
        val now = OffsetDateTime.now(clock)
        if (brawlStarsApiMaintenanceJpaRepository.existsByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(now, now)) {
            return
        }
        val endTime = now.plusHours(DEFAULT_DURATION_HOURS)
        brawlStarsApiMaintenanceJpaRepository.save(
            BrawlStarsApiMaintenanceEntity(
                startTime = now,
                endTime = endTime,
            ),
        )
        log.warn("[BrawlStars API 점검] 자동 점검 등록 startTime={} endTime={}", now, endTime)
    }
}
