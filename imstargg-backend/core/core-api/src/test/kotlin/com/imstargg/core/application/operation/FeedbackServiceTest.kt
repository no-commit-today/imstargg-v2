package com.imstargg.core.application.operation

import com.imstargg.CoreApiContextTest
import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Import(FeedbackServiceTest.FixedClockConfig::class)
class FeedbackServiceTest(
    private val sut: FeedbackService,
    private val cleanUp: CleanUp,
    private val feedbackJpaRepository: FeedbackJpaRepository,
) : CoreApiContextTest() {
    @TestConfiguration
    class FixedClockConfig {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))
    }

    companion object {
        val NOW: Instant = Instant.parse("2026-05-22T10:00:00Z")
    }

    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Test
    fun `요청한 값이 그대로 저장되고 식별자가 부여된다`() {
        // when
        val result =
            sut.submit(
                rating = FeedbackRating.VERY_SATISFIED,
                content = "만족합니다",
            )

        // then
        assertThat(result.id).isNotNull
        assertThat(result.rating).isEqualTo(FeedbackRating.VERY_SATISFIED)
        assertThat(result.content).isEqualTo("만족합니다")
        assertThat(result.submittedAt).isEqualTo(NOW)
        assertThat(result.checked).isFalse

        val saved = feedbackJpaRepository.findById(result.id).orElseThrow()
        assertThat(saved.rating).isEqualTo(FeedbackRating.VERY_SATISFIED)
        assertThat(saved.content).isEqualTo("만족합니다")
        assertThat(saved.submittedAt).isEqualTo(NOW)
    }

    @Test
    fun `content는 trim 되어 저장된다`() {
        // when
        val result =
            sut.submit(
                rating = FeedbackRating.SATISFIED,
                content = "  의견 내용  ",
            )

        // then
        assertThat(result.content).isEqualTo("의견 내용")
        val saved = feedbackJpaRepository.findById(result.id).orElseThrow()
        assertThat(saved.content).isEqualTo("의견 내용")
    }

    @Test
    fun `submittedAt은 주입된 Clock 시각으로 세팅된다`() {
        // when
        val result =
            sut.submit(
                rating = FeedbackRating.NEUTRAL,
                content = "보통",
            )

        // then
        assertThat(result.submittedAt).isEqualTo(NOW)
        val saved = feedbackJpaRepository.findById(result.id).orElseThrow()
        assertThat(saved.submittedAt).isEqualTo(NOW)
    }
}
