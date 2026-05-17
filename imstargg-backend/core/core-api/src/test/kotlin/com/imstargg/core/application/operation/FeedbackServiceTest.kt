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
    private val ipHasher = IpHasher(IpHasherProperties(ipSalt = "salt"))
    private val repository = mockk<FeedbackRepository>()
    private val sut = FeedbackService(ipHasher, repository)

    @Test
    fun `IP를 해시한 뒤 저장한다`() {
        // given
        val expectedHash = ipHasher.hash("1.2.3.4")
        every { repository.save(any()) } answers {
            val cmd = firstArg<FeedbackCreate>()
            Feedback(
                id = 1L,
                category = cmd.category,
                content = cmd.content,
                email = cmd.email,
                ipHash = cmd.ipHash,
                checked = false,
            )
        }

        // when
        val result =
            sut.submit(
                category = FeedbackCategory.BUG,
                content = "버그",
                email = "a@b.com",
                ip = "1.2.3.4",
            )

        // then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.ipHash).isEqualTo(expectedHash)
        verify {
            repository.save(
                FeedbackCreate(
                    category = FeedbackCategory.BUG,
                    content = "버그",
                    email = "a@b.com",
                    ipHash = expectedHash,
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
                ipHash = cmd.ipHash,
                checked = false,
            )
        }

        // when
        sut.submit(
            category = FeedbackCategory.BUG,
            content = "  버그 내용  ",
            email = null,
            ip = "1.2.3.4",
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
            Feedback(id = 1L, category = cmd.category, content = cmd.content, email = cmd.email, ipHash = cmd.ipHash, checked = false)
        }

        // when
        sut.submit(
            category = FeedbackCategory.BUG,
            content = "버그",
            email = "  ",
            ip = "1.2.3.4",
        )

        // then
        verify {
            repository.save(
                match { it.email == null },
            )
        }
    }
}
