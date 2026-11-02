package com.imstargg.client.brawlstars

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @property batchConcurrency [BrawlStarsClient.getPlayersRecentBattles] 가 클라이언트 전체에서 동시에 띄우는 요청 수 상한
 */
@ConfigurationProperties(prefix = "app.client.brawlstars")
data class BrawlStarsClientProperties(
    val apiKeys: List<String>,
    val url: String,
    val batchConcurrency: Int = 5,
)
