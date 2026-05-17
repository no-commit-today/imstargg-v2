package com.imstargg.core.application.operation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test

class IpHasherTest {
    private val sut = IpHasher(IpHasherProperties(ipSalt = "test-salt"))

    @Test
    fun `같은 IP는 같은 해시를 만든다`() {
        // when
        val first = sut.hash("1.2.3.4")
        val second = sut.hash("1.2.3.4")

        // then
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `다른 IP는 다른 해시를 만든다`() {
        // when
        val a = sut.hash("1.2.3.4")
        val b = sut.hash("1.2.3.5")

        // then
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `해시는 64자리 hex 문자열이다`() {
        // when
        val hash = sut.hash("1.2.3.4")

        // then
        assertThat(hash).hasSize(64)
        assertThat(hash).matches("^[0-9a-f]{64}$")
    }

    @Test
    fun `salt가 다르면 해시가 달라진다`() {
        // given
        val other = IpHasher(IpHasherProperties(ipSalt = "different-salt"))

        // when
        val a = sut.hash("1.2.3.4")
        val b = other.hash("1.2.3.4")

        // then
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `salt가 공백이면 초기화에 실패한다`() {
        // when
        val ex =
            catchThrowableOfType(IllegalArgumentException::class.java) {
                IpHasher(IpHasherProperties(ipSalt = "  "))
            }

        // then
        assertThat(ex).isNotNull()
    }
}
