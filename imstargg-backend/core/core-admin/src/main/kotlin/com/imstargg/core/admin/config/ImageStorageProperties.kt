package com.imstargg.core.admin.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.image-storage")
data class ImageStorageProperties(
    val bucket: String,
    val region: String,
    val cdnBaseUrl: String,
)
