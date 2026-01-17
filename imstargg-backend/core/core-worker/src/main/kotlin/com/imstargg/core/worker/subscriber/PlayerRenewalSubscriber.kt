package com.imstargg.core.worker.subscriber

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders
import com.imstargg.core.worker.application.PlayerRenewalService
import com.imstargg.infrastructure.pubsub.PlayerRenewalEvent
import org.slf4j.LoggerFactory
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class PlayerRenewalSubscriber(
    private val playerRenewalService: PlayerRenewalService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @ServiceActivator(inputChannel = "playerRenewalInputChannel")
    fun handleMessage(
        @Payload event: PlayerRenewalEvent,
        @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) message: BasicAcknowledgeablePubsubMessage,
    ) {
        log.info("[플레이어 갱신] 시작 tag={}", event.tag)

        playerRenewalService.renew(event.tag)

        message.ack()
        log.info("[플레이어 갱신] 완료 tag={}", event.tag)
    }
}
