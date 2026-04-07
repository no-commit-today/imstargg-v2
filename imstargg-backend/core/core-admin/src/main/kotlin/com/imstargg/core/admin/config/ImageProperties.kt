package com.imstargg.core.admin.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.image")
data class ImageProperties(
    val baseUrl: String,
)
