package com.imstargg.core.batch.support.coreapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import java.io.IOException

class CoreApiClientTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val sut = CoreApiClient(restClient = builder.build())

    @Test
    fun `2xx 응답이면 ACCEPTED 를 반환하고 태그를 URL 인코딩해 호출한다`() {
        // given
        server
            .expect(requestTo("/v1/players/%23AAA/renewal"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.OK))

        // when
        val result = sut.renewPlayer("#AAA")

        // then
        assertThat(result).isEqualTo(PlayerRenewalResult.ACCEPTED)
        server.verify()
    }

    @Test
    fun `409 응답이면 ALREADY_IN_PROGRESS 를 반환한다`() {
        // given
        server.expect(requestTo("/v1/players/%23AAA/renewal")).andRespond(withStatus(HttpStatus.CONFLICT))

        // when
        val result = sut.renewPlayer("#AAA")

        // then
        assertThat(result).isEqualTo(PlayerRenewalResult.ALREADY_IN_PROGRESS)
    }

    @Test
    fun `410 응답이면 PLAYER_DELETED 를 반환한다`() {
        // given
        server.expect(requestTo("/v1/players/%23AAA/renewal")).andRespond(withStatus(HttpStatus.GONE))

        // when
        val result = sut.renewPlayer("#AAA")

        // then
        assertThat(result).isEqualTo(PlayerRenewalResult.PLAYER_DELETED)
    }

    @Test
    fun `503 응답이면 API_UNDER_MAINTENANCE 를 반환한다`() {
        // given
        server
            .expect(requestTo("/v1/players/%23AAA/renewal"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        // when
        val result = sut.renewPlayer("#AAA")

        // then
        assertThat(result).isEqualTo(PlayerRenewalResult.API_UNDER_MAINTENANCE)
    }

    @Test
    fun `그 외 오류 응답이면 FAILED 를 반환한다`() {
        // given
        server
            .expect(requestTo("/v1/players/%23AAA/renewal"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        // when
        val result = sut.renewPlayer("#AAA")

        // then
        assertThat(result).isEqualTo(PlayerRenewalResult.FAILED)
    }

    @Test
    fun `네트워크 오류가 발생하면 FAILED 를 반환한다`() {
        // given
        server
            .expect(requestTo("/v1/players/%23AAA/renewal"))
            .andRespond(withException(IOException("connection refused")))

        // when
        val result = sut.renewPlayer("#AAA")

        // then
        assertThat(result).isEqualTo(PlayerRenewalResult.FAILED)
    }
}
