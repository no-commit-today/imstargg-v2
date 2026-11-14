package com.imstargg.core.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.CacheErrorHandler

private val log = KotlinLogging.logger {}

/**
 * Redis 등 캐시 백엔드에서 예외가 발생해도 요청 흐름은 정상 동작하도록 폴백 처리.
 * 예외를 다시 던지지 않으므로 `@Cacheable` 메서드는 캐시 미스로 간주되어 실제 로직(DB 조회) 이 실행된다.
 */
class RedisFallbackCacheErrorHandler : CacheErrorHandler {
    override fun handleCacheGetError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
    ) {
        log.warn(exception) { "cache get error cache=${cache.name} key=$key" }
    }

    override fun handleCachePutError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
        value: Any?,
    ) {
        log.warn(exception) { "cache put error cache=${cache.name} key=$key" }
    }

    override fun handleCacheEvictError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
    ) {
        log.warn(exception) { "cache evict error cache=${cache.name} key=$key" }
    }

    override fun handleCacheClearError(
        exception: RuntimeException,
        cache: Cache,
    ) {
        log.warn(exception) { "cache clear error cache=${cache.name}" }
    }
}
