package com.imstargg

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.worker.consumer.ClubRenewalMessageConsumer
import com.imstargg.core.worker.consumer.PlayerRenewalMessageConsumer
import com.imstargg.infrastructure.db.core.test.TestJpaConfig
import com.ninjasquad.springmockk.MockkBean
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestConstructor
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 실제 `BrawlStarsClient` 로 HTTP Service 그룹 배선(요청 팩토리·타임아웃·인터셉터·processor)을 확인한다.
 *
 * [CoreWorkerContextTest] 는 `BrawlStarsClient` 를 mock 으로 바꾸므로 상속하지 않고, 그 클래스의 애노테이션과
 * SQS consumer mock 만 복제한다. 브롤스타즈 API 대신 로컬 `HttpServer` 스텁에 붙는다.
 */
@Tag("context")
@SpringBootTest
@Import(TestJpaConfig::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class BrawlStarsClientHttpWiringContextTest(
    private val brawlStarsClient: BrawlStarsClient,
    private val rateLimiterRegistry: RateLimiterRegistry,
) {
    @MockkBean(relaxed = true)
    private lateinit var playerRenewalMessageConsumer: PlayerRenewalMessageConsumer

    @MockkBean(relaxed = true)
    private lateinit var clubRenewalMessageConsumer: ClubRenewalMessageConsumer

    @Test
    fun `응답 대기 초과는 HC5 read-timeout 으로 ResourceAccessException 이 된다`() {
        // when
        val thrown =
            assertTimeoutPreemptively(Duration.ofSeconds(5)) {
                catchThrowableOfType(ResourceAccessException::class.java) { brawlStarsClient.getPlayerRecentBattles("#SLOW") }
            }

        // then
        assertThat(thrown).hasRootCauseInstanceOf(SocketTimeoutException::class.java)
    }

    @Test
    fun `요청에 Bearer 키를 붙이고 태그를 퍼센트 인코딩해 보낸다`() {
        // given
        lastAuthorization.set(null)
        lastRawPath.set(null)

        // when
        brawlStarsClient.getPlayerRecentBattles("#A")

        // then
        assertThat(lastAuthorization.get()).isEqualTo("Bearer wiring-key")
        assertThat(lastRawPath.get()).isEqualTo("/v1/players/%23A/battlelog")
    }

    @Test
    fun `HTTP 요청마다 brawlstars 제한기 허가를 하나씩 쓴다`() {
        // given
        val rateLimiter = rateLimiterRegistry.rateLimiter("brawlstars")
        val before = rateLimiter.metrics.availablePermissions

        // when
        brawlStarsClient.getPlayerRecentBattles("#A")
        brawlStarsClient.getPlayersRecentBattles(listOf("#A", "#B"))

        // then
        assertThat(before - rateLimiter.metrics.availablePermissions).isEqualTo(3)
    }

    @Test
    fun `503 은 HTTP 클라이언트 층에서 재시도하지 않는다`() {
        // given
        unavailableCount.set(0)

        // when
        val thrown =
            catchThrowableOfType(HttpServerErrorException.ServiceUnavailable::class.java) {
                brawlStarsClient.getPlayerRecentBattles("#UNAVAILABLE")
            }

        // then
        assertThat(thrown).isNotNull()
        assertThat(unavailableCount).hasValue(1)
    }

    companion object {
        private const val SLOW_PATH = "/v1/players/%23SLOW/battlelog"
        private const val UNAVAILABLE_PATH = "/v1/players/%23UNAVAILABLE/battlelog"
        private val BATTLELOG_BODY = """{"items":[],"paging":null}""".toByteArray()

        private val unavailableCount = AtomicInteger()
        private val lastAuthorization = AtomicReference<String?>()
        private val lastRawPath = AtomicReference<String?>()

        // 기본 실행기는 디스패처 스레드 하나라, 잠든 SLOW 요청이 뒤 요청을 막지 않도록 실행기를 따로 둔다
        private val serverExecutor = Executors.newCachedThreadPool()
        private val server =
            HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                createContext("/") { exchange -> handle(exchange) }
                executor = serverExecutor
                start()
            }

        // 디코딩된 path 가 아니라 rawPath 로 분기한다 — 태그의 # 이 %23 으로 인코딩돼 왔는지 봐야 한다
        private fun handle(exchange: HttpExchange) {
            try {
                when (exchange.requestURI.rawPath) {
                    SLOW_PATH -> respondSlowly(exchange)
                    UNAVAILABLE_PATH -> {
                        unavailableCount.incrementAndGet()
                        exchange.sendResponseHeaders(503, -1)
                    }
                    else -> {
                        lastAuthorization.set(exchange.requestHeaders.getFirst(HttpHeaders.AUTHORIZATION))
                        lastRawPath.set(exchange.requestURI.rawPath)
                        // HttpServer 는 Content-Type 을 붙이지 않는다 — 빠지면 RestClient 가 본문을 읽을 변환기를 찾지 못한다
                        exchange.responseHeaders.set(HttpHeaders.CONTENT_TYPE, "application/json")
                        exchange.sendResponseHeaders(200, BATTLELOG_BODY.size.toLong())
                        exchange.responseBody.write(BATTLELOG_BODY)
                    }
                }
            } finally {
                exchange.close()
            }
        }

        // read-timeout(500ms)보다 오래 잠든다. 그 사이 클라이언트가 끊거나(IOException) 서버가 멈추면(InterruptedException) 응답은 버린다
        private fun respondSlowly(exchange: HttpExchange) {
            try {
                Thread.sleep(3_000)
                exchange.sendResponseHeaders(200, -1)
            } catch (ignored: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (ignored: IOException) {
                // 클라이언트가 이미 끊은 연결이다
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("app.client.brawlstars.url") { "http://127.0.0.1:${server.address.port}" }
            registry.add("app.client.brawlstars.api-keys") { "wiring-key" }
            registry.add("spring.http.clients.read-timeout") { "500ms" }
            registry.add("resilience4j.ratelimiter.instances.brawlstars.limit-for-period") { "1000" }
            registry.add("resilience4j.ratelimiter.instances.brawlstars.limit-refresh-period") { "1h" }
            registry.add("resilience4j.ratelimiter.instances.brawlstars.timeout-duration") { "0s" }
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            server.stop(0)
            serverExecutor.shutdownNow()
        }
    }
}
