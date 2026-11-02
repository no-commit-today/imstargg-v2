package com.imstargg.client.brawlstars

import feign.RequestInterceptor
import feign.RequestTemplate
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {}

internal class BrawlStarsApiKeyInterceptor(
    private val keys: List<String>,
) : RequestInterceptor {
    private val keyIndex = AtomicInteger()

    init {
        log.debug { "Brawl Stars API keys count: ${keys.size}" }
        require(keys.isNotEmpty()) { "Brawl Stars API key is required" }
    }

    override fun apply(requestTemplate: RequestTemplate) {
        val index = keyIndex.getAndIncrement()
        val key = keys[index % keys.size]
        requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer $key")
    }
}
