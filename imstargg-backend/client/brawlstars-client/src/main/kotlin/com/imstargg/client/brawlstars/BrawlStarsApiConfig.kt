package com.imstargg.client.brawlstars

import feign.RequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class BrawlStarsApiConfig {
    @Bean
    fun brawlStarsApiKeyInterceptor(properties: BrawlStarsClientProperties): RequestInterceptor =
        BrawlStarsApiKeyInterceptor(properties.apiKeys)
}
