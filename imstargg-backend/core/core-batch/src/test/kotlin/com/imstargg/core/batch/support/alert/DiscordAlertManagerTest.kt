package com.imstargg.core.batch.support.alert

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import java.io.IOException

class DiscordAlertManagerTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val sut = DiscordAlertManager(builder, WEBHOOK_URL)
    private val command =
        AlertCommand
            .builder()
            .warn()
            .title("제목")
            .content("내용")
            .build()

    @Test
    fun `전송에 성공하면 예외가 없다`() {
        // given
        server
            .expect(requestTo(WEBHOOK_URL))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.NO_CONTENT))

        // when & then
        assertThatCode { sut.alert(command) }.doesNotThrowAnyException()
        server.verify()
    }

    @Test
    fun `4xx 응답이면 받은 예외를 그대로 던진다`() {
        // given
        server.expect(requestTo(WEBHOOK_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST))

        // when & then
        assertThatThrownBy { sut.alert(command) }
            .isInstanceOf(HttpClientErrorException::class.java)
            .satisfies({ ex -> assertThat(ex.stackTraceToString()).doesNotContain(SECRET) })
    }

    @Test
    fun `503 응답이면 ServiceUnavailable 예외를 그대로 던진다`() {
        // given
        server.expect(requestTo(WEBHOOK_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        // when & then
        assertThatThrownBy { sut.alert(command) }
            .isInstanceOf(HttpServerErrorException.ServiceUnavailable::class.java)
    }

    @Test
    fun `연결 오류의 예외에는 웹훅 주소가 없다`() {
        // given
        server.expect(requestTo(WEBHOOK_URL)).andRespond(withException(IOException("connect timed out")))

        // when & then
        assertThatThrownBy { sut.alert(command) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasNoCause()
            .satisfies({ ex -> assertThat(ex.stackTraceToString()).doesNotContain(SECRET) })
    }

    @Test
    fun `주소가 깨져 있어도 예외에 웹훅 주소가 없다`() {
        // given
        val sut = DiscordAlertManager(RestClient.builder(), "\"$WEBHOOK_URL\"")

        // when & then
        assertThatThrownBy { sut.alert(command) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasNoCause()
            .satisfies({ ex ->
                assertThat(ex.message).doesNotContain(SECRET).doesNotContain(HOST)
                assertThat(ex.stackTraceToString()).doesNotContain(SECRET).doesNotContain(HOST)
            })
    }

    companion object {
        private const val HOST = "discord.test"
        private const val SECRET = "secret-token"
        private const val WEBHOOK_URL = "https://$HOST/api/webhooks/1/$SECRET"
    }
}
