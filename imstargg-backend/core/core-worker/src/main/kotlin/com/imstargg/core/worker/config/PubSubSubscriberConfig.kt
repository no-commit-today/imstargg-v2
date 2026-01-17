package com.imstargg.core.worker.config

import com.google.cloud.spring.pubsub.core.PubSubTemplate
import com.google.cloud.spring.pubsub.integration.AckMode
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter
import com.imstargg.infrastructure.pubsub.ClubRenewalEvent
import com.imstargg.infrastructure.pubsub.ClubRenewalPubSubProperties
import com.imstargg.infrastructure.pubsub.PlayerRenewalEvent
import com.imstargg.infrastructure.pubsub.PlayerRenewalPubSubProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.PublishSubscribeChannel
import org.springframework.messaging.MessageChannel

@Configuration
class PubSubSubscriberConfig(
    private val playerRenewalPubSubProperties: PlayerRenewalPubSubProperties,
    private val clubRenewalPubSubProperties: ClubRenewalPubSubProperties,
) {
    @Bean
    fun playerRenewalInputChannel(): MessageChannel = PublishSubscribeChannel()

    @Bean
    fun playerRenewalChannelAdapter(
        @Qualifier("playerRenewalInputChannel") inputChannel: MessageChannel,
        pubSubTemplate: PubSubTemplate,
    ): PubSubInboundChannelAdapter {
        val adapter = PubSubInboundChannelAdapter(pubSubTemplate, playerRenewalPubSubProperties.subscription.name)
        adapter.setOutputChannel(inputChannel)
        adapter.setAckMode(AckMode.MANUAL)
        adapter.setPayloadType(PlayerRenewalEvent::class.java)
        return adapter
    }

    @Bean
    fun clubRenewalInputChannel(): MessageChannel = PublishSubscribeChannel()

    @Bean
    fun clubRenewalChannelAdapter(
        @Qualifier("clubRenewalInputChannel") inputChannel: MessageChannel,
        pubSubTemplate: PubSubTemplate,
    ): PubSubInboundChannelAdapter {
        val adapter = PubSubInboundChannelAdapter(pubSubTemplate, clubRenewalPubSubProperties.subscription.name)
        adapter.setOutputChannel(inputChannel)
        adapter.setAckMode(AckMode.MANUAL)
        adapter.setPayloadType(ClubRenewalEvent::class.java)
        return adapter
    }
}
