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
 * 정지하지 않도록 connect/read 타임아웃(3s/5s)을 명시한다. 참고로 외부 API 클라이언트 기본값은
 * `support:http-client` 의 connect 3s/read 10s 다 — 이 클라이언트는 그 설정을 쓰지 않고 요청 팩토리를 직접 만든다.
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
