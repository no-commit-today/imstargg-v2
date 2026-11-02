package com.imstargg.client.brawlstars

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse
import java.net.URI
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class BrawlStarsRateLimitInterceptorTest {
    private val rateLimiter =
        RateLimiter.of(
            "t",
            RateLimiterConfig
                .custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofHours(1))
                .timeoutDuration(Duration.ZERO)
                .build(),
        )
    private val executionCount = AtomicInteger()
    private val execution =
        ClientHttpRequestExecution { _, _ ->
            executionCount.incrementAndGet()
            MockClientHttpResponse(ByteArray(0), HttpStatus.OK)
        }
    private val sut = BrawlStarsRateLimitInterceptor(rateLimiter)

    @Test
    fun `요청마다 허가를 하나씩 쓴다`() {
        // given
        assertThat(rateLimiter.metrics.availablePermissions).isEqualTo(2)

        // when
        sut.intercept(request(), ByteArray(0), execution)

        // then
        assertThat(rateLimiter.metrics.availablePermissions).isEqualTo(1)
    }

    @Test
    fun `허가가 없으면 RequestNotPermitted 를 던지고 요청을 보내지 않는다`() {
        // given
        repeat(2) { sut.intercept(request(), ByteArray(0), execution) }

        // when
        val thrown = catchThrowableOfType(RequestNotPermitted::class.java) { sut.intercept(request(), ByteArray(0), execution) }

        // then
        assertThat(thrown).isNotNull()
        assertThat(executionCount).hasValue(2)
    }

    private fun request() = MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/v1/brawlers"))
}
