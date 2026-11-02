package com.imstargg.client.brawlstars

import io.github.resilience4j.ratelimiter.RateLimiter
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

/**
 * HTTP 요청마다 레이트리밋 허가를 하나씩 받는다. 대기 시간 안에 허가를 못 받으면 resilience4j `RequestNotPermitted` 를 그대로 던지고
 * 요청을 보내지 않는다.
 *
 * `@RateLimiter` 애노테이션(aspect)을 쓰지 않는다 — aspect 는 쓰는 앱마다 AspectJ 가 클래스패스에 있어야 걸리고(core-batch 는 data-jpa 를 거쳐
 * 우연히 들어온다) 빈을 거치지 않는 호출에는 걸리지 않지만, 인터셉터는 RestClient 를 지나는 모든 요청에 걸린다.
 * 그룹 설정에서 가장 바깥(먼저)에 등록해 호출 로그의 소요 시간에 허가 대기가 섞이지 않게 한다.
 */
internal class BrawlStarsRateLimitInterceptor(
    private val rateLimiter: RateLimiter,
) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        RateLimiter.waitForPermission(rateLimiter)
        return execution.execute(request, body)
    }
}
