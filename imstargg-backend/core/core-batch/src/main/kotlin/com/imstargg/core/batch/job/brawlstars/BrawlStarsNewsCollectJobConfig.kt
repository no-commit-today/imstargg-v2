package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.job.brawlstars.task.BrawlStarsNewsCollectTask
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 브롤스타즈 공식 블로그 글을 읽어 저장하고 알리는 잡.
 *
 * 스텝에 `BrawlStarsApiTasklet` 을 쓰지 않는다. 그 클래스는 브롤스타즈 API 의 503(점검)을 성공으로
 * 넘기는 장치인데, 이 잡이 읽는 곳은 API 가 아니라 블로그라서 쓰면 블로그 실패가 가려진다.
 * 수집 중 예외는 그대로 올라가 잡이 실패하고, 알림은 `JobFailureAlertListener` 가 보낸다.
 *
 * 파라미터가 없어 `RunIdIncrementer` 가 없으면 같은 잡의 두 번째 실행이 거부된다.
 */
@Configuration
class BrawlStarsNewsCollectJobConfig(
    private val jobRepository: JobRepository,
    private val task: BrawlStarsNewsCollectTask,
) {
    companion object {
        const val JOB_NAME = "brawlStarsNewsCollectJob"
        const val STEP_NAME = "brawlStarsNewsCollectStep"
    }

    @Bean(JOB_NAME)
    fun brawlStarsNewsCollectJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(brawlStarsNewsCollectStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(STEP_NAME)
    fun brawlStarsNewsCollectStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.collect()
                RepeatStatus.FINISHED
            })
            .build()
}
