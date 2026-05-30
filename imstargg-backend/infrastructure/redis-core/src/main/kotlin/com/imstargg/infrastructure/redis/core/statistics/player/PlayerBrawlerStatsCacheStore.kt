package com.imstargg.infrastructure.redis.core.statistics.player

import com.imstargg.core.enums.BattleType
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate

/**
 * 플레이어 브롤러 통계 캐시. 플레이어별 Redis Hash 로 보관한다.
 * - key  : playerBrawlerStats:v1:{playerTag}
 * - field: {battleType.name}:{startBattleDate}
 * 플레이어 갱신 시 evict(=DEL) 한 번으로 그 플레이어의 모든 시즌·타입 캐시를 제거한다.
 * Redis 장애는 모두 삼켜 호출측(읽기 재계산, 쓰기 무효화)을 깨뜨리지 않는다.
 */
@Component
class PlayerBrawlerStatsCacheStore(
    private val redisTemplate: StringRedisTemplate,
    private val codec: PlayerBrawlerStatsCacheCodec,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun find(
        playerTag: String,
        battleType: BattleType,
        startBattleDate: LocalDate,
    ): PlayerBrawlerStatsCacheValue? =
        runCatching {
            redisTemplate
                .opsForHash<String, String>()
                .get(key(playerTag), field(battleType, startBattleDate))
                ?.let { codec.decode(it) }
        }.onFailure { log.warn("[PlayerBrawlerStatsCache] find 실패 tag={}", playerTag, it) }
            .getOrNull()

    fun put(
        playerTag: String,
        battleType: BattleType,
        startBattleDate: LocalDate,
        value: PlayerBrawlerStatsCacheValue,
    ) {
        runCatching {
            val key = key(playerTag)
            redisTemplate.opsForHash<String, String>().put(key, field(battleType, startBattleDate), codec.encode(value))
            redisTemplate.expire(key, TTL)
        }.onFailure { log.warn("[PlayerBrawlerStatsCache] put 실패 tag={}", playerTag, it) }
    }

    fun evict(playerTag: String) {
        runCatching {
            redisTemplate.delete(key(playerTag))
        }.onFailure { log.warn("[PlayerBrawlerStatsCache] evict 실패 tag={}", playerTag, it) }
    }

    private fun key(playerTag: String): String = "$KEY_PREFIX$playerTag"

    private fun field(
        battleType: BattleType,
        startBattleDate: LocalDate,
    ): String = "${battleType.name}:$startBattleDate"

    companion object {
        private const val KEY_PREFIX = "playerBrawlerStats:v1:"
        private val TTL: Duration = Duration.ofHours(1)
    }
}
