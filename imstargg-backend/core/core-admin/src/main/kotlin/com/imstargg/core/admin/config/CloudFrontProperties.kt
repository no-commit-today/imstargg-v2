package com.imstargg.core.admin.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.aws.cloudfront")
data class CloudFrontProperties(
    val baseUrl: String,
)
