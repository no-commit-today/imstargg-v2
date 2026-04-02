package com.imstargg.infrastructure.aws.sqs

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.aws.sqs.renewal")
data class RenewalSqsProperties(
    val playerQueueName: String,
    val clubQueueName: String,
)
