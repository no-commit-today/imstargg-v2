package com.imstargg.infrastructure.aws.sqs

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(RenewalSqsProperties::class)
class RenewalSqsConfig
