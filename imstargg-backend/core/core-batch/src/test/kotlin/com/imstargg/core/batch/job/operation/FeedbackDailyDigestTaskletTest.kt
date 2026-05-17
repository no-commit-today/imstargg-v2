package com.imstargg.core.batch.job.operation

import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class FeedbackDailyDigestTaskletTest {
    private val fixedNow = Instant.parse("2026-05-17T12:00:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneId.of("UTC"))

    @Test
    fun `0건이면 알림을 보내지 않는다`() {
        // given
        val repository = mockk<FeedbackJpaRepository>()
        every {
            repository.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())
        } returns emptyList()
        val alertManager = mockk<AlertManager>(relaxed = true)
        val sut = FeedbackDailyDigestTasklet(repository, alertManager, clock)

        // when
        sut.run()

        // then
        verify(exactly = 0) { alertManager.alert(any<AlertCommand>()) }
    }

    @Test
    fun `24시간 윈도우 카운트와 이메일 카운트를 알림으로 보낸다`() {
        // given
        val repository = mockk<FeedbackJpaRepository>()
        every {
            repository.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any())
        } returns
            listOf(
                feedback(FeedbackCategory.BUG, email = "a@b.com"),
                feedback(FeedbackCategory.BUG, email = null),
                feedback(FeedbackCategory.FEATURE_REQUEST, email = null),
                feedback(FeedbackCategory.ETC, email = null),
                feedback(FeedbackCategory.ETC, email = null),
            )
        val alertManager = mockk<AlertManager>(relaxed = true)
        val sut = FeedbackDailyDigestTasklet(repository, alertManager, clock)
        val captured = slot<AlertCommand>()
        every { alertManager.alert(capture(captured)) } returns Unit

        // when
        sut.run()

        // then
        verify(exactly = 1) { alertManager.alert(any<AlertCommand>()) }
        val cmd = captured.captured
        assertThat(cmd.title).isEqualTo("📨 지난 24시간 피드백 5건")
        assertThat(cmd.content).contains("• BUG 2건")
        assertThat(cmd.content).contains("• FEATURE_REQUEST 1건")
        assertThat(cmd.content).contains("• ETC 2건")
        assertThat(cmd.content).contains("✉️ 이메일 응답 요청 1건")
    }

    private fun feedback(
        category: FeedbackCategory,
        email: String?,
    ): FeedbackEntity = FeedbackEntity(category = category, content = "x", email = email, ipHash = "h")
}
