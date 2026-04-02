package com.imstargg.core.batch.support.alert

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class AlertConfig {
    @Bean
    @ConditionalOnProperty(prefix = "app.alert", name = ["type"], havingValue = "NO_OPS", matchIfMissing = true)
    fun noOpsAlertManager(): AlertManager = NoOpsAlertManager()

    @Bean
    @ConditionalOnProperty(prefix = "app.alert", name = ["type"], havingValue = "DISCORD")
    fun discordAlertManager(
        discordAlertProperties: DiscordAlertProperties,
        restClientBuilder: RestClient.Builder,
    ): AlertManager = DiscordAlertManager(restClientBuilder, discordAlertProperties.webhookUrl!!)
}
