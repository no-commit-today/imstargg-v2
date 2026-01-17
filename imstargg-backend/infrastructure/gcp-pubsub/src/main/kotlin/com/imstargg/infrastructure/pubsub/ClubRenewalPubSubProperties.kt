package com.imstargg.infrastructure.pubsub

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.pubsub.club-renewal")
data class ClubRenewalPubSubProperties(
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
