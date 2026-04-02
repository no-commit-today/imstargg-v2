package com.imstargg.infrastructure.aws.s3

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AwsS3Properties::class)
class AwsS3PropertiesConfig
