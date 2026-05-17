package com.imstargg.core.api.support

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ClientIpResolverTest {
    private val sut = ClientIpResolver()

    @Test
    fun `X-Forwarded-For 헤더가 있으면 첫 토큰을 반환한다`() {
        // given
        val request = mockk<HttpServletRequest>()
        every { request.getHeader("X-Forwarded-For") } returns "1.2.3.4, 10.0.0.1"

        // when
        val ip = sut.resolve(request)

        // then
        assertThat(ip).isEqualTo("1.2.3.4")
    }

    @Test
    fun `X-Forwarded-For가 없으면 RemoteAddr를 반환한다`() {
        // given
        val request = mockk<HttpServletRequest>()
        every { request.getHeader("X-Forwarded-For") } returns null
        every { request.remoteAddr } returns "9.9.9.9"

        // when
        val ip = sut.resolve(request)

        // then
        assertThat(ip).isEqualTo("9.9.9.9")
    }
}
