package com.imstargg.core.config

import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.CacheErrorHandler

/**
 * Redis 등 캐시 백엔드에서 예외가 발생해도 요청 흐름은 정상 동작하도록 폴백 처리.
 * 예외를 다시 던지지 않으므로 `@Cacheable` 메서드는 캐시 미스로 간주되어 실제 로직(DB 조회) 이 실행된다.
 */
class RedisFallbackCacheErrorHandler : CacheErrorHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handleCacheGetError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
    ) {
        log.warn("cache get error cache={} key={}", cache.name, key, exception)
    }

    override fun handleCachePutError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
        value: Any?,
    ) {
        log.warn("cache put error cache={} key={}", cache.name, key, exception)
    }

    override fun handleCacheEvictError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
    ) {
        log.warn("cache evict error cache={} key={}", cache.name, key, exception)
    }

    override fun handleCacheClearError(
        exception: RuntimeException,
        cache: Cache,
    ) {
        log.warn("cache clear error cache={}", cache.name, exception)
    }
}
