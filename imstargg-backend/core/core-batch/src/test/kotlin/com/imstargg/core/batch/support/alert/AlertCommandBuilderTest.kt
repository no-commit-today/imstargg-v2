package com.imstargg.core.batch.support.alert

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AlertCommandBuilderTest {
    @Test
    fun `warn 타입으로 빌드 성공`() {
        // given
        val title = "경고 알림"
        val content = "경고 내용"

        // when
        val result =
            AlertCommandBuilder()
                .warn()
                .title(title)
                .content(content)
                .build()

        // then
        assertThat(result.title).isEqualTo("⚠️ $title")
        assertThat(result.content).isEqualTo(content)
        assertThat(result.ex).isNull()
    }

    @Test
    fun `error 타입으로 빌드 성공`() {
        // given
        val title = "에러 알림"
        val ex = RuntimeException("에러 발생")

        // when
        val result =
            AlertCommandBuilder()
                .error()
                .title(title)
                .ex(ex)
                .build()

        // then
        assertThat(result.title).isEqualTo("🚨 $title")
        assertThat(result.ex).isEqualTo(ex)
    }

    @Test
    fun `title이 없으면 예외발생`() {
        // given
        val sut =
            AlertCommandBuilder()
                .warn()

        // when, then
        assertThatThrownBy { sut.build() }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `content과 exception이 없어도 빌드 성공`() {
        // given
        val title = "알림"

        // when
        val result =
            AlertCommandBuilder()
                .title(title)
                .warn()
                .build()

        // then
        assertThat(result.content).isNull()
        assertThat(result.ex).isNull()
    }
}
