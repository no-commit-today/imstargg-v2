package com.imstargg.client.brawlstars

import feign.RequestInterceptor
import feign.RequestTemplate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import java.util.concurrent.atomic.AtomicInteger

internal class BrawlStarsApiKeyInterceptor(
    private val keys: List<String>,
) : RequestInterceptor {
    private val log = LoggerFactory.getLogger(BrawlStarsApiKeyInterceptor::class.java)
    private val keyIndex = AtomicInteger()

    init {
        log.debug("Brawl Stars API keys count: {}", keys.size)
        require(keys.isNotEmpty()) { "Brawl Stars API key is required" }
    }

    override fun apply(requestTemplate: RequestTemplate) {
        val index = keyIndex.getAndIncrement()
        val key = keys[index % keys.size]
        requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer $key")
    }
}
