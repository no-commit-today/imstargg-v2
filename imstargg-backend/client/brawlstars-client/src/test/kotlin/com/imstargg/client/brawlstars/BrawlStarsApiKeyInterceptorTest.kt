package com.imstargg.client.brawlstars

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse
import java.net.URI

class BrawlStarsApiKeyInterceptorTest {
    @Test
    fun `키를 순서대로 돌려 Authorization Bearer 헤더로 붙인다`() {
        // given
        val sut = BrawlStarsApiKeyInterceptor(listOf("k1", "k2"))
        val sentAuthorizations = mutableListOf<String?>()
        val execution =
            ClientHttpRequestExecution { request, _ ->
                sentAuthorizations.add(request.headers.getFirst(HttpHeaders.AUTHORIZATION))
                MockClientHttpResponse(ByteArray(0), HttpStatus.OK)
            }

        // when
        repeat(3) {
            sut.intercept(MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost/v1/brawlers")), ByteArray(0), execution)
        }

        // then
        assertThat(sentAuthorizations).containsExactly("Bearer k1", "Bearer k2", "Bearer k1")
    }

    @Test
    fun `키가 없으면 생성에 실패한다`() {
        // when
        val thrown = catchThrowableOfType(IllegalArgumentException::class.java) { BrawlStarsApiKeyInterceptor(emptyList()) }

        // then
        assertThat(thrown).isNotNull()
    }
}
