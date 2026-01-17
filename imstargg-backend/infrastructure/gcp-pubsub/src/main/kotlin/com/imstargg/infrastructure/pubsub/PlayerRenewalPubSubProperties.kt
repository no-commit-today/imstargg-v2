package com.imstargg.infrastructure.pubsub

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.pubsub.player-renewal")
data class PlayerRenewalPubSubProperties(
    val topic: TopicProperties,
    val subscription: SubscriptionProperties,
) {
    data class TopicProperties(
        val name: String,
    )

    data class SubscriptionProperties(
        val name: String,
    )
}
