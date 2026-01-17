package com.imstargg.infrastructure.pubsub

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.pubsub.subscription.player-renewal")
data class PlayerRenewalSubscriptionProperties(
    val name: String,
)
