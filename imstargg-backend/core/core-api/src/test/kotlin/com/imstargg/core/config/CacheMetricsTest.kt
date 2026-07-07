package com.imstargg.core.config

import com.imstargg.CoreApiContextTest
import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager

/**
 * 캐시 메트릭이 Prometheus 로 노출되는지 검증한다.
 *
 * Caffeine 만 직접 검증한다 — CI(pr-test.yml)에는 Redis 서비스가 없어(Postgres 만 Testcontainers 로 제공)
 * `redisCacheManager` 를 직접 호출하면 연결 실패한다. Redis `statistics` 캐시의 통계 수집은
 * [com.imstargg.core.config.CacheConfig] 의 `enableStatistics()` 로 켜지며, 실 Redis 환경에서
 * Boot 의 RedisCacheMeterBinderProvider 가 동일하게 노출한다 (기존 통계 테스트가 Redis 부재를
 * 견디는 것과 같은 이유로, 여기서도 라이브 Redis 를 요구하지 않는다).
 */
class CacheMetricsTest(
    private val meterRegistry: MeterRegistry,
    private val cacheManager: CacheManager,
) : CoreApiContextTest() {
    @Test
    fun `caffeine 캐시 메트릭이 등록되어 있다`() {
        // given - 캐시 접근으로 통계 발생
        cacheManager.getCache(CacheNames.BRAWLER)!!.also {
            it.put("probe", "value")
            it.get("probe")
        }

        // when // then
        assertThat(meterRegistry.find("cache.gets").tag("cache", CacheNames.BRAWLER).meters()).isNotEmpty()
    }
}
