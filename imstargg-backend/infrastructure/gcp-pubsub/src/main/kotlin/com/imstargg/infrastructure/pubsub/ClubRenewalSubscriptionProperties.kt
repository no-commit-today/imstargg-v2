package com.imstargg.infrastructure.pubsub

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.pubsub.subscription.club-renewal")
data class ClubRenewalSubscriptionProperties(
    val name: String,
)
