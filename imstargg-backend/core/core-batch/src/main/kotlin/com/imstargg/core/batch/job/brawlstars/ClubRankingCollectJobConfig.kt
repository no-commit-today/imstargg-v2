package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.job.brawlstars.task.ClubRankingCollectTask
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class ClubRankingCollectJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val task: ClubRankingCollectTask,
) {
    companion object {
        const val JOB_NAME = "clubRankingCollectJob"
        private const val STEP_NAME = "clubRankingCollectStep"
    }

    @Bean(JOB_NAME)
    fun clubRankingCollectJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(clubRankingCollectStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(STEP_NAME)
    fun clubRankingCollectStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    task.collect()
                },
                txManager,
            ).build()
}
