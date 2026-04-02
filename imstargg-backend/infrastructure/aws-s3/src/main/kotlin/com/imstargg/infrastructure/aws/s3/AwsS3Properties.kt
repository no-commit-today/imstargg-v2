package com.imstargg.infrastructure.aws.s3

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.aws.s3")
data class AwsS3Properties(
    val bucket: String,
    val cdnBaseUrl: String = "",
)
