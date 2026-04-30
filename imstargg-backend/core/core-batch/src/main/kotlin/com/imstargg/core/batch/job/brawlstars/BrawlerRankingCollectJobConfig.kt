package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.job.brawlstars.task.BrawlerRankingCollectTask
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
class BrawlerRankingCollectJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val task: BrawlerRankingCollectTask,
) {
    companion object {
        const val JOB_NAME = "brawlerRankingCollectJob"
        private const val STEP_NAME = "brawlerRankingCollectStep"
    }

    @Bean(JOB_NAME)
    fun brawlerRankingCollectJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(brawlerRankingCollectStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(STEP_NAME)
    fun brawlerRankingCollectStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    task.collect()
                },
                txManager,
            ).build()
}
