package com.imstargg.core.batch.support.alert

import com.imstargg.CoreBatchApplication
import com.imstargg.core.batch.job.HealthCheckJobConfig
import com.imstargg.core.batch.job.brawlstars.BattleEventSyncJobConfig
import com.imstargg.infrastructure.db.core.test.TestJpaConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.InvalidJobParametersException
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.support.GenericApplicationContext
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Supplier

/**
 * 실제 core-batch 앱을 잡 실행 모드로 띄워, 운영과 같은 두 실패 경로에서 알림이 나가는지 확인한다.
 * - 잡이 시작되기 전의 실패(필수 파라미터 누락): `ApplicationFailedEvent`
 * - 잡이 시작된 뒤의 실패(스텝 예외): `JobExecutionEvent`
 */
@Tag("context")
class JobFailureAlertBootTest {
    private val alertManager = RecordingAlertManager()

    @Test
    fun `필수 파라미터 없이 잡을 실행해 잡이 시작되기 전에 앱이 실패하면 한 번 알린다`() {
        // when & then
        assertThatThrownBy { run(BattleEventSyncJobConfig.JOB_NAME) }
            .rootCause()
            .isInstanceOf(InvalidJobParametersException::class.java)
        assertThat(alertManager.commands).singleElement().satisfies({
            assertThat(it.title).contains("[${BattleEventSyncJobConfig.JOB_NAME}] 실패")
            assertThat(it.content).contains("InvalidJobParametersException")
        })
    }

    @Test
    fun `실행한 잡이 실패로 끝나면 한 번 알린다`() {
        // when
        run(FAILING_JOB_NAME).close()

        // then
        assertThat(alertManager.commands).singleElement().satisfies({
            assertThat(it.title).contains("[$FAILING_JOB_NAME] 실패")
            assertThat(it.content).contains("FAILED").contains(FAILURE_MESSAGE)
        })
    }

    @Test
    fun `실행한 잡이 완료되면 알리지 않는다`() {
        // when
        run(HealthCheckJobConfig.JOB_NAME).close()

        // then
        assertThat(alertManager.commands).isEmpty()
    }

    private fun run(jobName: String): ConfigurableApplicationContext =
        SpringApplicationBuilder(CoreBatchApplication::class.java, TestJpaConfig::class.java, FailingJobConfig::class.java)
            .initializers(
                ApplicationContextInitializer<ConfigurableApplicationContext> { context ->
                    (context as GenericApplicationContext).registerBean(
                        "recordingAlertManager",
                        AlertManager::class.java,
                        Supplier<AlertManager> { alertManager },
                        { it.isPrimary = true },
                    )
                },
            ).run(
                "--spring.batch.job.enabled=true",
                "--spring.batch.job.name=$jobName",
                "--storage.datasource.core.jdbc-url=jdbc:h2:mem:job-failure-alert;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;",
            )

    private class RecordingAlertManager : AlertManager {
        val commands: MutableList<AlertCommand> = CopyOnWriteArrayList()

        override fun alert(alertCommand: AlertCommand) {
            commands.add(alertCommand)
        }
    }

    /** 컴포넌트 스캔에 잡히지 않도록 `@Configuration` 을 달지 않는다(다른 컨텍스트 테스트에 잡이 섞이지 않게). */
    class FailingJobConfig {
        @Bean(FAILING_JOB_NAME)
        fun failingJob(jobRepository: JobRepository): Job =
            JobBuilder(FAILING_JOB_NAME, jobRepository)
                .start(
                    StepBuilder("jobFailureAlertTestStep", jobRepository)
                        .tasklet({ _, _ -> throw IllegalStateException(FAILURE_MESSAGE) })
                        .build(),
                ).build()
    }

    companion object {
        private const val FAILING_JOB_NAME = "jobFailureAlertTestJob"
        private const val FAILURE_MESSAGE = "의도한 스텝 실패"
    }
}
