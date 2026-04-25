package com.imstargg.core.batch.support.alert

import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient

internal class DiscordAlertManager(
    restClientBuilder: RestClient.Builder,
    private val webhookUrl: String,
) : AlertManager {
    private val log = LoggerFactory.getLogger(javaClass)
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
            log.error("Discord 알람 전송 실패", ex)
        }
    }
}
