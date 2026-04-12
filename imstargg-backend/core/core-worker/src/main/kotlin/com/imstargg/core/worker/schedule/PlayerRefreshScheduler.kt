package com.imstargg.core.worker.schedule

import com.imstargg.core.worker.application.PlayerRefreshService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app.schedule.player-refresh", name = ["enabled"], havingValue = "true")
class PlayerRefreshScheduler(
    private val playerRefreshService: PlayerRefreshService,
) {
    @Scheduled(fixedDelayString = "\${app.schedule.player-refresh.interval}")
    fun refresh() {
        playerRefreshService.refresh()
    }
}
