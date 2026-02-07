package com.imstargg.core.infrastructure.redis

import com.imstargg.core.application.renewal.RenewalProducer
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.infrastructure.redis.core.RenewalStreamProperties
import com.imstargg.infrastructure.redis.core.event.ClubRenewalEvent
import com.imstargg.infrastructure.redis.core.event.PlayerRenewalEvent
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisRenewalProducer(
    private val redisTemplate: StringRedisTemplate,
    private val renewalStreamProperties: RenewalStreamProperties,
) : RenewalProducer {
    override fun producePlayerRenewal(tag: BrawlStarsTag) {
        val record: ObjectRecord<String, PlayerRenewalEvent> =
            StreamRecords
                .newRecord()
                .ofObject(PlayerRenewalEvent(tag.value))
                .withStreamKey(renewalStreamProperties.playerRenewal.key)
        redisTemplate.opsForStream<String, String>().add(record)
    }

    override fun produceClubRenewal(tag: BrawlStarsTag) {
        val record: ObjectRecord<String, ClubRenewalEvent> =
            StreamRecords
                .newRecord()
                .ofObject(ClubRenewalEvent(tag.value))
                .withStreamKey(renewalStreamProperties.clubRenewal.key)
        redisTemplate.opsForStream<String, String>().add(record)
    }
}
