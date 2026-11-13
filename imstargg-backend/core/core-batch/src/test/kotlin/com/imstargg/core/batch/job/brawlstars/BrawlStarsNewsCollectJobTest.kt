package com.imstargg.core.batch.job.brawlstars

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.job.brawlstars.task.BrawlStarsNewsCollectTask
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException

/**
 * 잡의 배선만 본다. 수집 로직은 [BrawlStarsNewsCollectTask] 의 테스트가 맡는다.
 */
class BrawlStarsNewsCollectJobTest(
    private val jobOperator: JobOperator,
    @param:Qualifier(BrawlStarsNewsCollectJobConfig.JOB_NAME) private val brawlStarsNewsCollectJob: Job,
) : CoreBatchContextTest() {
    @MockkBean
    private lateinit var task: BrawlStarsNewsCollectTask

    @Test
    fun `잡이 수집을 한 번 부르고 끝난다`() {
        // given
        every { task.collect() } just runs

        // when
        val execution = jobOperator.start(brawlStarsNewsCollectJob, JobParameters())

        // then
        assertThat(execution.status).isEqualTo(BatchStatus.COMPLETED)
        verify(exactly = 1) { task.collect() }
    }

    @Test
    fun `파라미터 없이 두 번 실행할 수 있다`() {
        // given
        every { task.collect() } just runs

        // when
        val first = jobOperator.start(brawlStarsNewsCollectJob, JobParameters())
        val second = jobOperator.start(brawlStarsNewsCollectJob, JobParameters())

        // then
        assertThat(first.status).isEqualTo(BatchStatus.COMPLETED)
        assertThat(second.status).isEqualTo(BatchStatus.COMPLETED)
    }

    @Test
    fun `수집이 예외를 던지면 잡이 실패한다`() {
        // given
        every { task.collect() } throws
            HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                HttpHeaders(),
                ByteArray(0),
                null,
            )

        // when
        val execution = jobOperator.start(brawlStarsNewsCollectJob, JobParameters())

        // then
        assertThat(execution.status).isEqualTo(BatchStatus.FAILED)
    }
}
