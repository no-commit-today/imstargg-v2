package com.imstargg.infrastructure.pubsub

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.pubsub.topic.club-renewal")
data class ClubRenewalTopicProperties(
    val name: String,
)