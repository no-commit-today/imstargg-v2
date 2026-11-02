package com.imstargg.client.brawlstars

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {}

/**
 * 키 여러 개를 요청마다 순서대로 돌려 `Authorization: Bearer <key>` 를 붙인다.
 * 그룹 설정에서 `ApiLoggingInterceptor` 보다 안쪽(뒤)에 등록해 키가 호출 로그에 남지 않게 한다.
 */
internal class BrawlStarsApiKeyInterceptor(
    private val keys: List<String>,
) : ClientHttpRequestInterceptor {
    private val keyIndex = AtomicInteger()

    init {
        log.debug { "Brawl Stars API keys count: ${keys.size}" }
        require(keys.isNotEmpty()) { "Brawl Stars API key is required" }
    }

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        // getAndIncrement 는 int 를 넘으면 음수로 돌아가므로 floorMod 로 인덱스를 0 이상으로 둔다
        val key = keys[Math.floorMod(keyIndex.getAndIncrement(), keys.size)]
        request.headers.set(HttpHeaders.AUTHORIZATION, "Bearer $key")
        return execution.execute(request, body)
    }
}
