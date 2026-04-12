package com.imstargg.core.worker.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.schedule.player-refresh")
data class PlayerRefreshScheduleProperties(
    val enabled: Boolean = false,
    val interval: Duration = Duration.ofMinutes(1),
    val playerCount: Int = 1,
)
