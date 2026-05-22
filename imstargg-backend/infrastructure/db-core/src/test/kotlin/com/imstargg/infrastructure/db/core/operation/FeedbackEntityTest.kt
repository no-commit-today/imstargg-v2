package com.imstargg.infrastructure.db.core.operation

import com.imstargg.core.enums.operation.FeedbackRating
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class FeedbackEntityTest {
    private val fixedSubmittedAt = Instant.parse("2026-05-22T10:00:00Z")

    @Test
    fun `check()를 호출하면 checked가 true가 된다`() {
        // given
        val sut =
            FeedbackEntity(
                rating = FeedbackRating.VERY_SATISFIED,
                content = "만족합니다",
                submittedAt = fixedSubmittedAt,
            )

        // when
        sut.check()

        // then
        assertThat(sut.checked).isTrue()
    }

    @Test
    fun `uncheck()를 호출하면 checked가 false가 된다`() {
        // given
        val sut =
            FeedbackEntity(
                rating = FeedbackRating.VERY_SATISFIED,
                content = "만족합니다",
                submittedAt = fixedSubmittedAt,
            )
        sut.check()

        // when
        sut.uncheck()

        // then
        assertThat(sut.checked).isFalse()
    }

    @Test
    fun `submittedAt은 생성자 인자 그대로 보관된다`() {
        // given & when
        val sut =
            FeedbackEntity(
                rating = FeedbackRating.NEUTRAL,
                content = "보통",
                submittedAt = fixedSubmittedAt,
            )

        // then
        assertThat(sut.submittedAt).isEqualTo(fixedSubmittedAt)
    }
}
