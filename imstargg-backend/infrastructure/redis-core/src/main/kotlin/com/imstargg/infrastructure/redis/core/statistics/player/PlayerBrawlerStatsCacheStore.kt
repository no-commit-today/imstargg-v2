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
 * - field: {battleType.name}:{startBattleDate}:{endBattleDate}
 * 플레이어 갱신 시 evict(=DEL) 한 번으로 그 플레이어의 모든 시즌·타입 캐시를 제거한다.
 *
 * 필드는 도메인(PlayerBrawlerStats)의 식별자인 두 LocalDate 경계(startBattleDate, endBattleDate)를 그대로
 * 반영한다. 날짜 granularity 인 이유는 도메인이 기간을 LocalDate 로 식별하기 때문이며, 호출이 시즌 경계로
 * 정렬돼 같은 타입의 두 조회가 동일 (startBattleDate, endBattleDate) 로 충돌하지 않음을 전제한다.
 * (sub-day/임의 구간을 키잉해야 하면 instant-precise 키로 재검토 필요.)
 *
 * TTL 은 해시 키 단위다 — put 마다 1h 로 슬라이딩 리셋되며 필드별 TTL 이 아니다. 정합성은 evict-on-update
 * 가 담당하고 TTL 은 백스톱이다. Redis 장애는 모두 삼켜 호출측(읽기 재계산, 쓰기 무효화)을 깨뜨리지 않으며,
 * 그 결과 evict 실패 시 최대 TTL(1h)까지 stale 한 뒤 자연 만료된다.
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
        endBattleDate: LocalDate,
    ): PlayerBrawlerStatsCacheValue? =
        runCatching {
            val field =
                field(
                    battleType = battleType,
                    startBattleDate = startBattleDate,
                    endBattleDate = endBattleDate,
                )
            redisTemplate
                .opsForHash<String, String>()
                .get(key(playerTag), field)
                ?.let { codec.decode(it) }
        }.onFailure { log.warn("[PlayerBrawlerStatsCache] find 실패 tag={}", playerTag, it) }
            .getOrNull()

    fun put(
        playerTag: String,
        battleType: BattleType,
        startBattleDate: LocalDate,
        endBattleDate: LocalDate,
        value: PlayerBrawlerStatsCacheValue,
    ) {
        runCatching {
            val key = key(playerTag)
            val field =
                field(
                    battleType = battleType,
                    startBattleDate = startBattleDate,
                    endBattleDate = endBattleDate,
                )
            redisTemplate.opsForHash<String, String>().put(key, field, codec.encode(value))
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
        endBattleDate: LocalDate,
    ): String = "${battleType.name}:$startBattleDate:$endBattleDate"

    companion object {
        private const val KEY_PREFIX = "playerBrawlerStats:v1:"
        private val TTL: Duration = Duration.ofHours(1)
    }
}
