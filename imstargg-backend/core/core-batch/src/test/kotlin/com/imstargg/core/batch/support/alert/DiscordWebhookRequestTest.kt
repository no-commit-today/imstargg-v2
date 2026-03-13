package com.imstargg.core.batch.support.alert

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DiscordWebhookRequestTest {
    @Test
    fun `content가 최대 길이를 초과할 때 자동으로 잘리고 후미에 메시지를 추가한다`() {
        // given
        val longContent = "a".repeat(2500)

        // when
        val result = DiscordWebhookRequest(longContent)

        // then
        val postfix = "\n...자세한 내용은 로그를 참고..."
        assertThat(result.content)
            .hasSize(2000)
            .endsWith(postfix)
    }

    @Test
    fun `AlertCommand를 기반으로 DiscordWebhookRequest를 생성할 수 있다`() {
        // given
        val alertCommand =
            AlertCommand(
                "테스트 제목",
                "테스트 내용",
                RuntimeException("테스트 예외"),
            )

        // when
        val result = DiscordWebhookRequest.from(alertCommand)

        // then
        assertThat(result.content)
            .contains("# 테스트 제목")
            .contains("[내용]\n테스트 내용")
            .contains("[예외]\njava.lang.RuntimeException: 테스트 예외")
    }

    @Test
    fun `AlertCommand에 내용이 없으면 생성된 콘텐츠에 내용 섹션이 없다`() {
        // given
        val alertCommand =
            AlertCommand(
                "테스트 제목",
                null,
                RuntimeException("테스트 예외"),
            )

        // when
        val result = DiscordWebhookRequest.from(alertCommand)

        // then
        assertThat(result.content)
            .contains("# 테스트 제목")
            .doesNotContain("[내용]")
    }

    @Test
    fun `AlertCommand에 예외가 없으면 생성된 콘텐츠에 예외 섹션이 없다`() {
        // given
        val alertCommand =
            AlertCommand(
                "테스트 제목",
                "테스트 내용",
                null,
            )

        // when
        val result = DiscordWebhookRequest.from(alertCommand)

        // then
        assertThat(result.content)
            .contains("# 테스트 제목")
            .contains("[내용]\n테스트 내용")
            .doesNotContain("[예외]")
    }
}
