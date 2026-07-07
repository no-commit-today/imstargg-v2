package com.imstargg.core.config

import com.imstargg.CoreApiContextTest
import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheManager

class CacheMetricsTest(
    private val meterRegistry: MeterRegistry,
    private val cacheManager: CacheManager,
    private val redisCacheManager: RedisCacheManager,
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

    @Test
    fun `redis statistics 캐시의 통계 수집이 활성화되어 있다`() {
        // given
        val cache = redisCacheManager.getCache(CacheNames.STATISTICS) as RedisCache

        // when
        cache.get("metrics-probe")

        // then
        assertThat(cache.statistics.gets).isGreaterThan(0)
    }
}
