package com.imstargg.core.worker.consumer

import com.imstargg.core.worker.application.PlayerRenewalService
import com.imstargg.infrastructure.redis.core.RenewalStreamProperties
import com.imstargg.infrastructure.redis.core.event.PlayerRenewalEvent
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.stereotype.Component

@Component
class PlayerRenewalConsumer(
    private val playerRenewalService: PlayerRenewalService,
    private val stringRedisTemplate: StringRedisTemplate,
    private val renewalStreamProperties: RenewalStreamProperties,
) : StreamListener<String, ObjectRecord<String, PlayerRenewalEvent>> {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onMessage(message: ObjectRecord<String, PlayerRenewalEvent>) {
        val event = message.value
        log.info("[플레이어 갱신] 시작 tag={}", event.tag)
        playerRenewalService.renew(event.tag)
        stringRedisTemplate
            .opsForStream<String, String>()
            .acknowledge(renewalStreamProperties.consumer.group, message)
        log.info("[플레이어 갱신] 완료 tag={}", event.tag)
    }
}
