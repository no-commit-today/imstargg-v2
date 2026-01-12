package com.imstargg.client.brawlstars

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.client.brawlstars")
data class BrawlStarsClientProperties(
    val keys: List<String>,
    val url: String,
)
