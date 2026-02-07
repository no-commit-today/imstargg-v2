package com.imstargg.infrastructure.redis.core

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.redis.stream")
data class RenewalStreamProperties(
    val playerRenewal: Stream,
    val clubRenewal: Stream,
    val consumer: Consumer,
) {
    data class Stream(
        val key: String,
    )

    data class Consumer(
        val group: String,
        val name: String,
    )
}
