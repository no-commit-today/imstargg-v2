package com.imstargg.infrastructure.db.core.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FeedbackEntityTest {
    @Test
    fun `check()를 호출하면 checked가 true가 된다`() {
        // given
        val sut =
            FeedbackEntity(
                category = FeedbackCategory.BUG,
                content = "버그 발생",
                email = null,
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
                category = FeedbackCategory.BUG,
                content = "버그 발생",
                email = null,
            )
        sut.check()

        // when
        sut.uncheck()

        // then
        assertThat(sut.checked).isFalse()
    }
}
