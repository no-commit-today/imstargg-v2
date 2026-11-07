package com.imstargg.core.batch.support.alert

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.client.RestClient

private val log = KotlinLogging.logger {}

internal class DiscordAlertManager(
    restClientBuilder: RestClient.Builder,
    private val webhookUrl: String,
) : AlertManager {
    private val restClient: RestClient = restClientBuilder.build()

    override fun alert(alertCommand: AlertCommand) {
        try {
            restClient
                .post()
                .uri(webhookUrl)
                .body(DiscordWebhookRequest.from(alertCommand))
                .retrieve()
                .body(Void::class.java)
        } catch (ex: Exception) {
            log.error(ex) { "Discord 알람 전송 실패" }
        }
    }
}
