package com.imstargg.core.worker.config

import com.imstargg.infrastructure.redis.core.RenewalStreamProperties
import com.imstargg.infrastructure.redis.core.event.ClubRenewalEvent
import com.imstargg.infrastructure.redis.core.event.PlayerRenewalEvent
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import java.time.Duration

@Configuration
class RedisStreamConsumerConfig(
    private val renewalStreamProperties: RenewalStreamProperties,
    private val redisConnectionFactory: RedisConnectionFactory,
    private val stringRedisTemplate: StringRedisTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun initializeConsumerGroups() {
        initializeConsumerGroup(renewalStreamProperties.playerRenewal.key)
        initializeConsumerGroup(renewalStreamProperties.clubRenewal.key)
    }

    private fun initializeConsumerGroup(streamKey: String) {
        try {
            stringRedisTemplate
                .opsForStream<String, String>()
                .createGroup(streamKey, renewalStreamProperties.consumer.group)
        } catch (e: Exception) {
            log.debug("Consumer group already exists for stream '{}': {}", streamKey, e.message)
        }
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun playerRenewalListenerContainer(
        playerRenewalConsumer: StreamListener<String, ObjectRecord<String, PlayerRenewalEvent>>,
    ): StreamMessageListenerContainer<String, ObjectRecord<String, PlayerRenewalEvent>> =
        createContainer(
            renewalStreamProperties.playerRenewal.key,
            PlayerRenewalEvent::class.java,
            playerRenewalConsumer,
        )

    @Bean(initMethod = "start", destroyMethod = "stop")
    fun clubRenewalListenerContainer(
        clubRenewalConsumer: StreamListener<String, ObjectRecord<String, ClubRenewalEvent>>,
    ): StreamMessageListenerContainer<String, ObjectRecord<String, ClubRenewalEvent>> =
        createContainer(
            renewalStreamProperties.clubRenewal.key,
            ClubRenewalEvent::class.java,
            clubRenewalConsumer,
        )

    private fun <T> createContainer(
        streamKey: String,
        targetType: Class<T>,
        listener: StreamListener<String, ObjectRecord<String, T>>,
    ): StreamMessageListenerContainer<String, ObjectRecord<String, T>> {
        val options =
            StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(2))
                .targetType(targetType)
                .build()
        val container = StreamMessageListenerContainer.create(redisConnectionFactory, options)
        container.receive(
            Consumer.from(renewalStreamProperties.consumer.group, renewalStreamProperties.consumer.name),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            listener,
        )
        return container
    }
}
