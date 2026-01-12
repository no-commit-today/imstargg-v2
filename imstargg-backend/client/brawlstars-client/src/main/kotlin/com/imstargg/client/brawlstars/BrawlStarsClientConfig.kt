package com.imstargg.client.brawlstars

import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableFeignClients(clients = [BrawlStarsApi::class])
internal class BrawlStarsClientConfig {
    @Bean
    fun brawlStarsClient(brawlStarsApi: BrawlStarsApi): BrawlStarsClient = BrawlStarsClient(brawlStarsApi)
}
