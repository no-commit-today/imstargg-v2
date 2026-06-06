package com.imstargg.core.batch.domain.operation

import com.imstargg.core.enums.operation.FeedbackRating
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FeedbackDigestMessageTest {
    @Test
    fun `0건이면 null을 반환한다`() {
        // when
        val result = FeedbackDigestMessage.build(emptyMap())

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `rating별 건수를 한국어 라벨로 포함한다`() {
        // given
        val counts =
            mapOf(
                FeedbackRating.VERY_SATISFIED to 4L,
                FeedbackRating.SATISFIED to 5L,
                FeedbackRating.NEUTRAL to 2L,
                FeedbackRating.DISSATISFIED to 1L,
                FeedbackRating.VERY_DISSATISFIED to 0L,
            )

        // when
        val result = FeedbackDigestMessage.build(counts)!!

        // then
        assertThat(result.title).isEqualTo("📨 지난 24시간 피드백 12건")
        assertThat(result.content).contains("• 매우 만족 4건")
        assertThat(result.content).contains("• 만족 5건")
        assertThat(result.content).contains("• 보통 2건")
        assertThat(result.content).contains("• 불만족 1건")
        assertThat(result.content).doesNotContain("매우 불만족")
    }

    @Test
    fun `0인 rating 라인은 생략한다`() {
        // given
        val counts =
            mapOf(
                FeedbackRating.VERY_SATISFIED to 0L,
                FeedbackRating.SATISFIED to 3L,
                FeedbackRating.NEUTRAL to 0L,
            )

        // when
        val result = FeedbackDigestMessage.build(counts)!!

        // then
        assertThat(result.title).isEqualTo("📨 지난 24시간 피드백 3건")
        assertThat(result.content).doesNotContain("매우 만족")
        assertThat(result.content).doesNotContain("보통")
        assertThat(result.content).contains("• 만족 3건")
    }
}
