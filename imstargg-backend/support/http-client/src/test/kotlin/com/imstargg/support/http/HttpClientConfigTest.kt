package com.imstargg.support.http

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.apache.hc.client5.http.config.Configurable
import org.apache.hc.core5.util.Timeout
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class HttpClientConfigTest {
    @Test
    fun `커넥션 임대 대기 상한은 10초다`() {
        // given
        val factory = factory()

        // when
        val connectionRequestTimeout = (factory.httpClient as Configurable).config.connectionRequestTimeout

        // then
        assertThat(connectionRequestTimeout).isEqualTo(Timeout.ofSeconds(10))
    }

    @Test
    fun `503 응답에 HC5 가 재시도하지 않아 요청은 한 번만 간다`() {
        // given
        val requestCount = AtomicInteger()
        val server =
            startServer { exchange ->
                requestCount.incrementAndGet()
                exchange.sendResponseHeaders(503, -1)
                exchange.close()
            }
        val client =
            RestClient
                .builder()
                .requestFactory(factory())
                .baseUrl("http://localhost:${server.address.port}")
                .build()

        try {
            // when
            val thrown =
                catchThrowableOfType(HttpServerErrorException.ServiceUnavailable::class.java) {
                    client
                        .get()
                        .uri("/")
                        .retrieve()
                        .toBodilessEntity()
                }

            // then
            assertThat(thrown).isNotNull()
            assertThat(requestCount).hasValue(1)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `한 라우트에 5개를 넘는 동시 요청이 커넥션 대기 없이 서버에 닿는다`() {
        // given
        val concurrency = 6
        val inFlight = AtomicInteger()
        val release = CountDownLatch(1)
        val server =
            startServer { exchange ->
                inFlight.incrementAndGet()
                release.await(10, TimeUnit.SECONDS)
                exchange.sendResponseHeaders(200, -1)
                exchange.close()
            }
        val client =
            RestClient
                .builder()
                .requestFactory(factory())
                .baseUrl("http://localhost:${server.address.port}")
                .build()

        try {
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                // when
                val futures =
                    List(concurrency) {
                        executor.submit<Unit> {
                            client
                                .get()
                                .uri("/")
                                .retrieve()
                                .toBodilessEntity()
                        }
                    }

                // then
                Awaitility
                    .await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted { assertThat(inFlight).hasValue(concurrency) }
                release.countDown()
                futures.forEach { it.get(5, TimeUnit.SECONDS) }
            }
        } finally {
            release.countDown()
            server.stop(0)
        }
    }

    @Test
    fun `자동 구성 빌더에 커스터마이저가 적용된다`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(HttpClientAutoConfiguration::class.java, ImperativeHttpClientAutoConfiguration::class.java),
            ).withUserConfiguration(HttpClientConfig::class.java)
            .withPropertyValues("spring.http.clients.imperative.factory=http-components")
            .run { context ->
                // given
                val builder = context.getBean(ClientHttpRequestFactoryBuilder::class.java)

                // when
                val factory = builder.build() as HttpComponentsClientHttpRequestFactory

                // then
                assertThat((factory.httpClient as Configurable).config.connectionRequestTimeout).isEqualTo(Timeout.ofSeconds(10))
            }
    }

    private fun factory() =
        HttpClientConfig()
            .httpComponentsCustomizer()
            .customize(ClientHttpRequestFactoryBuilder.httpComponents())
            .build(HttpClientSettings.defaults())

    // 기본 실행기는 디스패처 스레드 하나라 요청이 직렬화되므로, 동시 요청을 보려면 실행기를 따로 둔다
    private fun startServer(handler: (HttpExchange) -> Unit): HttpServer =
        HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
            createContext("/") { exchange -> handler(exchange) }
            executor = Executors.newCachedThreadPool()
            start()
        }
}
