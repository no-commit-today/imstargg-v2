package com.imstargg.client.brawlstars

import feign.RequestInterceptor
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(BrawlStarsClientProperties::class)
internal class BrawlStarsApiConfig {
    @Bean
    fun brawlStarsApiKeyInterceptor(properties: BrawlStarsClientProperties): RequestInterceptor =
        BrawlStarsApiKeyInterceptor(properties.keys)
}
