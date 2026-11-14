package com.imstargg.core.batch.support.alert

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.JobInstance
import org.springframework.batch.core.job.parameters.InvalidJobParametersException
import org.springframework.batch.core.job.parameters.JobParameter
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.boot.SpringApplication
import org.springframework.boot.batch.autoconfigure.JobExecutionEvent
import org.springframework.boot.context.event.ApplicationFailedEvent
import org.springframework.context.support.StaticApplicationContext
import org.springframework.mock.env.MockEnvironment
import java.time.LocalDate

class JobFailureAlertListenerTest {
    private val alertManager = mockk<AlertManager>(relaxUnitFun = true)
    private val environment =
        MockEnvironment()
            .withProperty("spring.batch.job.name", "battleEventSyncJob")
            .withProperty("HOSTNAME", "core-batch-pod-1")
    private val sut = JobFailureAlertListener(alertManager, environment)

    @Test
    fun `잡이 실패로 끝나면 잡 이름·파라미터·예외 첫 줄을 담아 한 번 알린다`() {
        // given
        val execution =
            jobExecution(
                jobName = "brawlerBattleRankStatsJob",
                status = BatchStatus.FAILED,
                failure = IllegalArgumentException("rank(19)는 최대 10 이어야 합니다.\n두 번째 줄"),
            )

        // when
        sut.onJobExecution(JobExecutionEvent(execution))

        // then
        val alert = slot<AlertCommand>()
        verify(exactly = 1) { alertManager.alert(capture(alert)) }
        assertThat(alert.captured.title).contains("[brawlerBattleRankStatsJob] 실패")
        assertThat(alert.captured.content)
            .contains("date=2026-11-09")
            .contains("FAILED")
            .contains("java.lang.IllegalArgumentException: rank(19)는 최대 10 이어야 합니다.")
            .doesNotContain("두 번째 줄")
            .contains("core-batch-pod-1")
    }

    @Test
    fun `잡이 완료되면 알리지 않는다`() {
        // given
        val execution = jobExecution(jobName = "brawlerBattleRankStatsJob", status = BatchStatus.COMPLETED, failure = null)

        // when
        sut.onJobExecution(JobExecutionEvent(execution))

        // then
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `잡이 시작되기 전에 앱이 실패하면 설정의 잡 이름·실행 인자·근본 예외를 담아 한 번 알린다`() {
        // given
        val cause = InvalidJobParametersException("The JobParameters do not contain required keys: [date]")
        val event =
            ApplicationFailedEvent(
                SpringApplication(),
                arrayOf("--job.name=battleEventSyncJob", "date=2026-11-09"),
                StaticApplicationContext(),
                IllegalStateException("Failed to execute ApplicationRunner", cause),
            )

        // when
        sut.onApplicationFailed(event)

        // then
        val alert = slot<AlertCommand>()
        verify(exactly = 1) { alertManager.alert(capture(alert)) }
        assertThat(alert.captured.title).contains("[battleEventSyncJob] 실패")
        assertThat(alert.captured.content)
            .contains("--job.name=battleEventSyncJob date=2026-11-09")
            .contains("InvalidJobParametersException: The JobParameters do not contain required keys: [date]")
            .contains("core-batch-pod-1")
    }

    @Test
    fun `알림 전송이 예외를 던져도 리스너는 예외를 밖으로 내지 않는다`() {
        // given
        every { alertManager.alert(any()) } throws IllegalStateException("webhook down")
        val execution =
            jobExecution(
                jobName = "brawlerBattleRankStatsJob",
                status = BatchStatus.FAILED,
                failure = IllegalArgumentException("boom"),
            )

        // when & then
        assertThatCode { sut.onJobExecution(JobExecutionEvent(execution)) }.doesNotThrowAnyException()
    }

    @Test
    fun `알림 본문을 만드는 중에 예외가 나도 리스너는 예외를 밖으로 내지 않는다`() {
        // given
        val brokenValue =
            object {
                override fun toString(): String = throw IllegalStateException("toString 실패")
            }
        val parameters = JobParameters(setOf(JobParameter("broken", brokenValue, Any::class.java)))
        val execution =
            jobExecution(
                jobName = "brawlerBattleRankStatsJob",
                status = BatchStatus.FAILED,
                failure = IllegalArgumentException("boom"),
                parameters = parameters,
            )

        // when & then
        assertThatCode { sut.onJobExecution(JobExecutionEvent(execution)) }.doesNotThrowAnyException()
    }

    private fun jobExecution(
        jobName: String,
        status: BatchStatus,
        failure: Throwable?,
        parameters: JobParameters =
            JobParameters(setOf(JobParameter("date", LocalDate.of(2026, 11, 9), LocalDate::class.java))),
    ): JobExecution =
        JobExecution(1L, JobInstance(1L, jobName), parameters).apply {
            this.status = status
            exitStatus = if (status == BatchStatus.COMPLETED) ExitStatus.COMPLETED else ExitStatus.FAILED
            failure?.let { addFailureException(it) }
        }
}
