package com.imstargg.infrastructure.pubsub

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.pubsub.topic.player-renewal")
data class PlayerRenewalTopicProperties(
    val name: String,
)