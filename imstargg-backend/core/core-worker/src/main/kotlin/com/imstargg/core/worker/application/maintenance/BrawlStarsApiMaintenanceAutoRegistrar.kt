package com.imstargg.core.worker.application.maintenance

import com.imstargg.infrastructure.db.core.brawlstars.system.BrawlStarsApiMaintenanceEntity
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiMaintenanceJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

private val log = KotlinLogging.logger {}

@Component
class BrawlStarsApiMaintenanceAutoRegistrar(
    private val brawlStarsApiMaintenanceJpaRepository: BrawlStarsApiMaintenanceJpaRepository,
    private val clock: Clock,
) {
    companion object {
        private const val DEFAULT_DURATION_HOURS = 1L
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
        log.warn { "[BrawlStars API 점검] 자동 점검 등록 startTime=$now endTime=$endTime" }
    }
}
