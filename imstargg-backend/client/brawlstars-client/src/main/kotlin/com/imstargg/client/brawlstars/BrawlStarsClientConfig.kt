package com.imstargg.client.brawlstars

import com.imstargg.support.http.ApiLoggingInterceptor
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.springframework.web.service.registry.ImportHttpServices

/**
 * 브롤스타즈 API HTTP Service 그룹([GROUP]) 구성.
 *
 * 요청 팩토리(Apache HttpClient 5)·타임아웃·커넥션 풀은 `support:http-client` 의 `http-client.yml`·`HttpClientConfig` 가
 * 자동 구성으로 그룹에 적용한다. 그룹별로 다르게 하려면 `spring.http.serviceclient.brawlstars.*` 로 오버라이드한다.
 * 여기서 `clientBuilder.requestFactory(...)` 를 부르면 그 설정이 모두 덮이므로 부르지 않는다.
 *
 * 인터셉터는 등록 순서대로 바깥 → 안으로 감싼다.
 * 1. [BrawlStarsRateLimitInterceptor] — 요청마다 제한기([RATE_LIMITER_NAME]) 허가를 받는다
 * 2. [ApiLoggingInterceptor] — 허가 대기를 뺀 응답 시간을 남긴다. 같은 인스턴스를 프록시 팩토리의 processor 로도 등록한다
 * 3. [BrawlStarsApiKeyInterceptor] — 로깅보다 안쪽이라 키가 로그에 남지 않는다
 */
@Configuration
@EnableConfigurationProperties(BrawlStarsClientProperties::class)
@ImportHttpServices(group = BrawlStarsClientConfig.GROUP, types = [BrawlStarsApi::class])
internal class BrawlStarsClientConfig {
    @Bean
    fun brawlStarsGroupConfigurer(
        properties: BrawlStarsClientProperties,
        rateLimiterRegistry: RateLimiterRegistry,
    ): RestClientHttpServiceGroupConfigurer =
        RestClientHttpServiceGroupConfigurer { groups ->
            groups.filterByName(GROUP).forEachGroup { _, clientBuilder, proxyFactoryBuilder ->
                val apiLogging = ApiLoggingInterceptor()
                clientBuilder
                    .baseUrl(properties.url)
                    .requestInterceptor(BrawlStarsRateLimitInterceptor(rateLimiterRegistry.rateLimiter(RATE_LIMITER_NAME)))
                    .requestInterceptor(apiLogging)
                    .requestInterceptor(BrawlStarsApiKeyInterceptor(properties.apiKeys))
                proxyFactoryBuilder.httpRequestValuesProcessor(apiLogging)
            }
        }

    @Bean
    fun brawlStarsClient(brawlStarsApi: BrawlStarsApi): BrawlStarsClient = BrawlStarsClient(brawlStarsApi)

    companion object {
        const val GROUP = "brawlstars"
        const val RATE_LIMITER_NAME = "brawlstars"
    }
}
