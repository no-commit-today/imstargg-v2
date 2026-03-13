package com.imstargg.core.batch.support.alert

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AlertCommandTest {
    @Test
    fun `예외가 없을 때 exStackTrace는 null을 반환한다`() {
        // given
        val sut = AlertCommand("Test Title", "Test Content", null)

        // when
        val result = sut.exStackTrace()

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `예외가 있을 때 exStackTrace는 스택트레이스를 반환한다`() {
        // given
        val exception = Exception("Test Exception")
        val sut = AlertCommand("Test Title", "Test Content", exception)

        // when
        val result = sut.exStackTrace()

        // then
        assertThat(result).contains("Test Exception")
    }
}
