package com.imstargg.core.batch.job.operation

import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class FeedbackDailyDigestJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val tasklet: FeedbackDailyDigestTasklet,
) {
    companion object {
        const val JOB_NAME = "feedbackDailyDigestJob"
        const val STEP_NAME = "feedbackDailyDigestStep"
    }

    @Bean(JOB_NAME)
    fun job(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(step())
            .build()

    @Bean(STEP_NAME)
    fun step(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                tasklet.run()
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
