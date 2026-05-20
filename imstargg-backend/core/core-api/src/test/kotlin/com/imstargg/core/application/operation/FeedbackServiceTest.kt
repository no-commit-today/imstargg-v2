package com.imstargg.core.application.operation

import com.imstargg.CoreApiContextTest
import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FeedbackServiceTest(
    private val sut: FeedbackService,
    private val cleanUp: CleanUp,
    private val feedbackJpaRepository: FeedbackJpaRepository,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Test
    fun `요청한 값이 그대로 저장되고 식별자가 부여된다`() {
        // when
        val result =
            sut.submit(
                category = FeedbackCategory.BUG,
                content = "버그",
                email = "a@b.com",
            )

        // then
        assertThat(result.id).isNotNull
        assertThat(result.category).isEqualTo(FeedbackCategory.BUG)
        assertThat(result.content).isEqualTo("버그")
        assertThat(result.email).isEqualTo("a@b.com")
        assertThat(result.checked).isFalse

        val saved = feedbackJpaRepository.findById(result.id).orElseThrow()
        assertThat(saved.content).isEqualTo("버그")
        assertThat(saved.email).isEqualTo("a@b.com")
    }

    @Test
    fun `content는 trim 되어 저장된다`() {
        // when
        val result =
            sut.submit(
                category = FeedbackCategory.BUG,
                content = "  버그 내용  ",
                email = null,
            )

        // then
        assertThat(result.content).isEqualTo("버그 내용")
        val saved = feedbackJpaRepository.findById(result.id).orElseThrow()
        assertThat(saved.content).isEqualTo("버그 내용")
    }

    @Test
    fun `빈 이메일은 null 로 정규화되어 저장된다`() {
        // when
        val result =
            sut.submit(
                category = FeedbackCategory.BUG,
                content = "버그",
                email = "  ",
            )

        // then
        assertThat(result.email).isNull()
        val saved = feedbackJpaRepository.findById(result.id).orElseThrow()
        assertThat(saved.email).isNull()
    }
}
