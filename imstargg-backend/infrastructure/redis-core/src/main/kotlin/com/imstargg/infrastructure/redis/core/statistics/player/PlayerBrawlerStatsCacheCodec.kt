package com.imstargg.infrastructure.redis.core.statistics.player

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

@Component
class PlayerBrawlerStatsCacheCodec {
    private val mapper =
        JsonMapper
            .builder()
            .addModule(KotlinModule.Builder().build())
            .build()

    fun encode(value: PlayerBrawlerStatsCacheValue): String = mapper.writeValueAsString(value)

    fun decode(json: String): PlayerBrawlerStatsCacheValue = mapper.readValue(json, PlayerBrawlerStatsCacheValue::class.java)
}
