package com.imstargg.support.http

import org.apache.hc.core5.util.Timeout
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.autoconfigure.ClientHttpRequestFactoryBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * HTTP Service 그룹(`@ImportHttpServices` — 외부 API 클라이언트)이 쓰는 HTTP 요청 팩토리 보정.
 *
 * - **자체 재시도 끔.** Apache HttpClient 5 는 기본으로 자체 재시도를 켠다 — `DefaultHttpRequestRetryStrategy` 가 429·503 응답과
 *   GET 의 일부 IOException 을 1초 간격으로 1회 재시도한다. 이 재시도는 클라이언트 아래(RestClient 요청 팩토리)에서 보이지 않게 일어나,
 *   클라이언트는 재시도하지 않는다는 결정과 충돌하고 503(점검) 감지도 1초 늦춘다. Boot 의 HC5 빌더는 이것을 끄지 않는다.
 * - **커넥션 풀 전체 [MAX_CONN_TOTAL]·라우트당 [MAX_CONN_PER_ROUTE].** HC5 기본(전체 25·라우트당 5)이면 한 호스트로 동시에 5개를 넘는 호출이
 *   풀에서 대기한다. 그룹 하나가 호스트 하나라 라우트당 값이 곧 동시 호출 상한이다. core-worker 의 동시 호출 상한은
 *   배치 5 + SQS 리스너 2개 × 기본 동시 처리 10 + 새로고침 1 = 26 이므로, 그보다 넉넉히 잡아 풀 대기가 생기지 않게 한다.
 * - **커넥션 임대 대기 [CONNECTION_REQUEST_TIMEOUT].** 풀이 가득 찼을 때 무한정(HC5 기본 3분) 기다리지 않고 10초 뒤 실패한다.
 *
 * 커넥션 TTL(`setConnectionTimeToLive`)과 `setValidateAfterInactivity` 는 쓰지 않는다 — 둘 다 내부에서 `setDefaultConnectionConfig` 로
 * 새 `ConnectionConfig` 를 만들어, Boot 가 먼저 넣은 connect·read 타임아웃(`spring.http.clients.*`)을 통째로 덮는다.
 * HC5 기본의 무활동 2초 뒤 stale 커넥션 검사는 그대로 남는다.
 *
 * 이 커스터마이저는 `ImperativeHttpClientAutoConfiguration` 이 만드는 `ClientHttpRequestFactoryBuilder` 빈에 적용되고,
 * 그 빈을 `HttpServiceClientAutoConfiguration` 이 그룹의 requestFactory 로 쓴다. factory 가 `http-components` 가 아니면 타입이 맞지 않아
 * 호출되지 않는다(`http-client.yml` 참고). 그룹 설정에서 `requestFactory(...)` 를 직접 부르면 이 설정이 모두 덮이므로 부르지 않는다.
 */
@Configuration
class HttpClientConfig {
    @Bean
    fun httpComponentsCustomizer(): ClientHttpRequestFactoryBuilderCustomizer<HttpComponentsClientHttpRequestFactoryBuilder> =
        ClientHttpRequestFactoryBuilderCustomizer { builder ->
            // with* 는 새 불변 빌더를 돌려준다 — 체인의 결과를 반환해야 설정이 남는다
            builder
                .withHttpClientCustomizer { it.disableAutomaticRetries() }
                .withConnectionManagerCustomizer { it.setMaxConnTotal(MAX_CONN_TOTAL).setMaxConnPerRoute(MAX_CONN_PER_ROUTE) }
                .withDefaultRequestConfigCustomizer { it.setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT) }
        }

    companion object {
        const val MAX_CONN_TOTAL = 100
        const val MAX_CONN_PER_ROUTE = 50
        val CONNECTION_REQUEST_TIMEOUT: Timeout = Timeout.ofSeconds(10)
    }
}
