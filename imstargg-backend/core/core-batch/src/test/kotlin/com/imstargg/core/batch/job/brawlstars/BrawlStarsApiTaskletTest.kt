package com.imstargg.core.batch.job.brawlstars

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException

class BrawlStarsApiTaskletTest {
    private val contribution = mockk<StepContribution>(relaxed = true)
    private val chunkContext = mockk<ChunkContext>(relaxed = true)

    @Test
    fun `action 이 성공하면 FINISHED 를 반환한다`() {
        // given
        var executed = false
        val sut =
            BrawlStarsApiTasklet("testJob") {
                executed = true
            }

        // when
        val result = sut.execute(contribution, chunkContext)

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED)
        assertThat(executed).isTrue()
    }

    @Test
    fun `BrawlStars API 점검 예외는 삼키고 FINISHED 를 반환한다`() {
        // given
        val sut =
            BrawlStarsApiTasklet("testJob") {
                throw serviceUnavailable()
            }

        // when
        val result = sut.execute(contribution, chunkContext)

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED)
    }

    @Test
    fun `ServiceUnavailable 이 아닌 예외는 전파한다`() {
        // given
        val sut =
            BrawlStarsApiTasklet("testJob") {
                throw IllegalStateException("boom")
            }

        // when
        val ex =
            catchThrowableOfType(IllegalStateException::class.java) {
                sut.execute(contribution, chunkContext)
            }

        // then
        assertThat(ex).isNotNull()
        assertThat(ex.message).isEqualTo("boom")
    }

    @Test
    fun `503 이 아닌 HTTP 오류는 전파한다`() {
        // given
        val sut =
            BrawlStarsApiTasklet("testJob") {
                throw internalServerError()
            }

        // when
        val ex =
            catchThrowableOfType(HttpServerErrorException.InternalServerError::class.java) {
                sut.execute(contribution, chunkContext)
            }

        // then
        assertThat(ex).isNotNull()
    }

    private fun serviceUnavailable(): HttpServerErrorException =
        HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders(), ByteArray(0), null)

    private fun internalServerError(): HttpServerErrorException =
        HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", HttpHeaders(), ByteArray(0), null)
}
