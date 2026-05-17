package com.imstargg.core.application.operation

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.operation.feedback")
data class IpHasherProperties(
    val ipSalt: String,
)
