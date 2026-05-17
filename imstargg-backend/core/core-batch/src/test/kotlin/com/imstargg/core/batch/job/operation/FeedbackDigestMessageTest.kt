package com.imstargg.core.batch.job.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FeedbackDigestMessageTest {
    @Test
    fun `0건이면 null을 반환한다`() {
        // when
        val result = FeedbackDigestMessage.build(emptyMap(), emailCount = 0)

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `카테고리별 건수와 이메일 응답 요청 라인을 포함한다`() {
        // given
        val counts =
            mapOf(
                FeedbackCategory.BUG to 2L,
                FeedbackCategory.FEATURE_REQUEST to 1L,
                FeedbackCategory.ETC to 2L,
            )

        // when
        val result = FeedbackDigestMessage.build(counts, emailCount = 1)!!

        // then
        assertThat(result.title).isEqualTo("📨 지난 24시간 피드백 5건")
        assertThat(result.content).contains("• BUG 2건")
        assertThat(result.content).contains("• FEATURE_REQUEST 1건")
        assertThat(result.content).contains("• ETC 2건")
        assertThat(result.content).contains("✉️ 이메일 응답 요청 1건")
    }

    @Test
    fun `0인 카테고리 라인은 생략한다`() {
        // given
        val counts =
            mapOf(
                FeedbackCategory.BUG to 0L,
                FeedbackCategory.FEATURE_REQUEST to 3L,
                FeedbackCategory.ETC to 0L,
            )

        // when
        val result = FeedbackDigestMessage.build(counts, emailCount = 0)!!

        // then
        assertThat(result.title).isEqualTo("📨 지난 24시간 피드백 3건")
        assertThat(result.content).doesNotContain("BUG")
        assertThat(result.content).doesNotContain("ETC")
        assertThat(result.content).doesNotContain("이메일")
        assertThat(result.content).contains("• FEATURE_REQUEST 3건")
    }
}
