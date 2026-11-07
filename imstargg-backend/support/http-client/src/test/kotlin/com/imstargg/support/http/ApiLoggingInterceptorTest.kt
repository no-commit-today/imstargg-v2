package com.imstargg.support.http

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import java.net.SocketTimeoutException

class ApiLoggingInterceptorTest {
    private val sut = ApiLoggingInterceptor()

    private val apiLogger = LoggerFactory.getLogger(SampleApi::class.java) as Logger
    private val appender = ListAppender<ILoggingEvent>().apply { start() }.also { apiLogger.addAppender(it) }

    @AfterEach
    fun tearDown() {
        apiLogger.detachAppender(appender)
    }

    @Test
    fun `프록시로 부른 호출은 호출한 Api 인터페이스의 로거로 Api#메서드 를 붙여 남긴다`() {
        // given
        val builder = RestClient.builder().baseUrl("http://localhost").requestInterceptor(sut)
        val server = MockRestServiceServer.bindTo(builder).build()
        val api = createApi(builder, registerProcessor = true)
        server.expect(requestTo("http://localhost/items?id=1")).andRespond(withSuccess())

        // when
        api.getItems(id = 1)

        // then
        assertThat(appender.list.map { it.formattedMessage })
            .singleElement()
            .asString()
            .startsWith("[SampleApi#getItems] GET /items?id=1 -> 200")
    }

    @Test
    fun `프로세서로 등록하지 않으면 호출이 실패한다`() {
        // given
        val builder = RestClient.builder().baseUrl("http://localhost").requestInterceptor(sut)
        MockRestServiceServer.bindTo(builder).build()
        val api = createApi(builder, registerProcessor = false)

        // when
        val thrown = catchThrowableOfType(IllegalStateException::class.java) { api.getItems(id = 1) }

        // then
        assertThat(thrown).hasMessageContaining("httpRequestValuesProcessor")
    }

    @Test
    fun `오류 상태 응답도 INFO 로 남긴다`() {
        // given
        val builder = RestClient.builder().baseUrl("http://localhost").requestInterceptor(sut)
        val server = MockRestServiceServer.bindTo(builder).build()
        val api = createApi(builder, registerProcessor = true)
        server.expect(requestTo("http://localhost/items?id=1")).andRespond(withStatus(HttpStatus.NOT_FOUND))

        // when
        val thrown = catchThrowableOfType(HttpClientErrorException.NotFound::class.java) { api.getItems(id = 1) }

        // then
        assertThat(thrown).isNotNull()
        assertThat(appender.list).singleElement().satisfies({ event ->
            assertThat(event.level).isEqualTo(Level.INFO)
            assertThat(event.formattedMessage).contains("-> 404")
        })
    }

    @Test
    fun `전송 예외는 WARN 으로 남기고 예외를 그대로 올린다`() {
        // given
        val builder = RestClient.builder().baseUrl("http://localhost").requestInterceptor(sut)
        val server = MockRestServiceServer.bindTo(builder).build()
        val api = createApi(builder, registerProcessor = true)
        server.expect(requestTo("http://localhost/items?id=1")).andRespond(withException(SocketTimeoutException()))

        // when
        val thrown = catchThrowableOfType(ResourceAccessException::class.java) { api.getItems(id = 1) }

        // then
        assertThat(thrown).hasRootCauseInstanceOf(SocketTimeoutException::class.java)
        assertThat(appender.list).singleElement().satisfies({ event ->
            assertThat(event.level).isEqualTo(Level.WARN)
            assertThat(event.formattedMessage).contains("-> SocketTimeoutException")
        })
    }

    @Test
    fun `헤더는 남기지 않는다`() {
        // given
        val builder = RestClient.builder().baseUrl("http://localhost").requestInterceptor(sut)
        val server = MockRestServiceServer.bindTo(builder).build()
        val api = createApi(builder, registerProcessor = true)
        server
            .expect(requestTo("http://localhost/secured"))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer secret"))
            .andRespond(withSuccess())

        // when
        api.getSecured(authorization = "Bearer secret")

        // then
        assertThat(appender.list).singleElement().satisfies({ event ->
            assertThat(event.formattedMessage).doesNotContain("secret")
        })
    }

    private fun createApi(
        builder: RestClient.Builder,
        registerProcessor: Boolean,
    ): SampleApi {
        val proxyFactoryBuilder = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build()))
        if (registerProcessor) {
            proxyFactoryBuilder.httpRequestValuesProcessor(sut)
        }
        return proxyFactoryBuilder.build().createClient(SampleApi::class.java)
    }

    interface SampleApi {
        @GetExchange("/items")
        fun getItems(
            @RequestParam id: Int,
        ): String

        @GetExchange("/secured")
        fun getSecured(
            @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        ): String
    }
}
