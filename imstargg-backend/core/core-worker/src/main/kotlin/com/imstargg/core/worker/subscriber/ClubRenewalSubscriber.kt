package com.imstargg.core.worker.subscriber

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders
import com.imstargg.core.worker.application.ClubRenewalService
import com.imstargg.infrastructure.pubsub.ClubRenewalEvent
import org.slf4j.LoggerFactory
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class ClubRenewalSubscriber(
    private val clubRenewalService: ClubRenewalService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @ServiceActivator(inputChannel = "clubRenewalInputChannel")
    fun handleMessage(
        @Payload event: ClubRenewalEvent,
        @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) message: BasicAcknowledgeablePubsubMessage,
    ) {
        log.info("[클럽 갱신] 시작 tag={}", event.tag)

        clubRenewalService.renew(event.tag)

        message.ack()
        log.info("[클럽 갱신] 완료 tag={}", event.tag)
    }
}
