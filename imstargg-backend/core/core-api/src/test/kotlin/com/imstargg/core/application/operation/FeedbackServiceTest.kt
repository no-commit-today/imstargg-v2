package com.imstargg.core.application.operation

import com.imstargg.core.domain.operation.Feedback
import com.imstargg.core.domain.operation.FeedbackCreate
import com.imstargg.core.enums.operation.FeedbackCategory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FeedbackServiceTest {
    private val repository = mockk<FeedbackRepository>()
    private val sut = FeedbackService(repository)

    @Test
    fun `요청한 값을 그대로 저장한다`() {
        // given
        every { repository.save(any()) } answers {
            val cmd = firstArg<FeedbackCreate>()
            Feedback(
                id = 1L,
                category = cmd.category,
                content = cmd.content,
                email = cmd.email,
                checked = false,
            )
        }

        // when
        val result =
            sut.submit(
                category = FeedbackCategory.BUG,
                content = "버그",
                email = "a@b.com",
            )

        // then
        assertThat(result.id).isEqualTo(1L)
        verify {
            repository.save(
                FeedbackCreate(
                    category = FeedbackCategory.BUG,
                    content = "버그",
                    email = "a@b.com",
                ),
            )
        }
    }

    @Test
    fun `content는 trim된다`() {
        // given
        every { repository.save(any()) } answers {
            val cmd = firstArg<FeedbackCreate>()
            Feedback(
                id = 1L,
                category = cmd.category,
                content = cmd.content,
                email = cmd.email,
                checked = false,
            )
        }

        // when
        sut.submit(
            category = FeedbackCategory.BUG,
            content = "  버그 내용  ",
            email = null,
        )

        // then
        verify {
            repository.save(
                match { it.content == "버그 내용" },
            )
        }
    }

    @Test
    fun `빈 이메일은 null로 정규화된다`() {
        // given
        every { repository.save(any()) } answers {
            val cmd = firstArg<FeedbackCreate>()
            Feedback(id = 1L, category = cmd.category, content = cmd.content, email = cmd.email, checked = false)
        }

        // when
        sut.submit(
            category = FeedbackCategory.BUG,
            content = "버그",
            email = "  ",
        )

        // then
        verify {
            repository.save(
                match { it.email == null },
            )
        }
    }
}
