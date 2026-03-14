package com.imstargg.core.worker.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableConfigurationProperties(StatsBattleScheduleProperties::class)
@ConditionalOnProperty(prefix = "app.schedule.stats-battle", name = ["enabled"], havingValue = "true")
class ScheduleConfig
