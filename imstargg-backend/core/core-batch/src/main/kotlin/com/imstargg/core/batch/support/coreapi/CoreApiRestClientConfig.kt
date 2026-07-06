package com.imstargg.core.batch.support.coreapi

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * core-api 호출용 [RestClient] 설정.
 *
 * 배치가 수백~천여 건을 순차 호출하므로 커넥션 1개가 행에 걸려도 배치 전체가 무기한
 * 정지하지 않도록 connect/read 타임아웃을 명시한다 — 값은 brawlstars-client
 * (connect 2100ms/read 5000ms)와 같은 자릿수 기준.
 */
@Configuration
class CoreApiRestClientConfig {
    @Bean
    fun coreApiRestClient(
        restClientBuilder: RestClient.Builder,
        @Value("\${app.core-api.url}") coreApiUrl: String,
    ): RestClient =
        restClientBuilder
            .baseUrl(coreApiUrl)
            .requestFactory(
                ClientHttpRequestFactoryBuilder.detect().build(
                    HttpClientSettings
                        .defaults()
                        .withConnectTimeout(Duration.ofSeconds(3))
                        .withReadTimeout(Duration.ofSeconds(5)),
                ),
            ).build()
}
