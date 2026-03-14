package com.imstargg.core.worker.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.schedule.stats-battle")
data class StatsBattleScheduleProperties(
    val enabled: Boolean = false,
    val interval: Duration = Duration.ofSeconds(60),
    val playerCount: Int = 10,
)
