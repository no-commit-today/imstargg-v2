package com.imstargg.core.batch.support.alert

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.alert")
data class AlertProperties(
    val type: AlertType,
)
