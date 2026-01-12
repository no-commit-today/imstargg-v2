package com.imstargg.client.brawlstars

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.client.brawlstars")
data class BrawlStarsClientProperties(
    val apiKeys: List<String>,
    val url: String,
)
