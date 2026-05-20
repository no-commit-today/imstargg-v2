package com.imstargg.core.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.cache.support.CompositeCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.kotlin.KotlinModule
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig : CachingConfigurer {
    @Bean
    fun caffeineCacheManager(): CaffeineCacheManager =
        CaffeineCacheManager(
            CacheNames.BRAWLER,
            CacheNames.BATTLE_EVENT,
        ).apply {
            setCaffeine(
                Caffeine
                    .newBuilder()
                    .maximumSize(10000)
                    .expireAfterWrite(300, TimeUnit.SECONDS)
                    .recordStats(),
            )
        }

    @Bean
    fun redisCacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val typeValidator =
            BasicPolymorphicTypeValidator
                .builder()
                .allowIfSubType("com.imstargg.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .build()
        val serializer =
            GenericJacksonJsonRedisSerializer
                .builder()
                .enableDefaultTyping(typeValidator)
                .customize { mapperBuilder ->
                    mapperBuilder
                        .addModule(KotlinModule.Builder().build())
                        .setDefaultTyping(StatisticsTypeResolverBuilder(typeValidator))
                }.build()
        val statisticsConfig =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .computePrefixWith { cacheName -> "$cacheName::" }
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer),
                ).disableCachingNullValues()
        return RedisCacheManager
            .builder(connectionFactory)
            .cacheDefaults(statisticsConfig)
            .initialCacheNames(setOf(CacheNames.STATISTICS))
            .disableCreateOnMissingCache()
            .build()
    }

    @Bean
    @Primary
    fun cacheManager(
        caffeineCacheManager: CaffeineCacheManager,
        redisCacheManager: RedisCacheManager,
    ): CacheManager =
        CompositeCacheManager(caffeineCacheManager, redisCacheManager).apply {
            setFallbackToNoOpCache(false)
        }

    override fun errorHandler(): CacheErrorHandler = RedisFallbackCacheErrorHandler()
}
