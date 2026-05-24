package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.job.statistics.ownership.BrawlerItemOwnershipStatsTask
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BrawlerItemOwnershipStatsJobConfig(
    private val jobRepository: JobRepository,
    private val task: BrawlerItemOwnershipStatsTask,
) {
    companion object {
        const val JOB_NAME = "brawlerItemOwnershipStatsJob"
        private const val STEP_NAME = "brawlerItemOwnershipStatsStep"
    }

    @Bean(JOB_NAME)
    fun brawlerItemOwnershipStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(brawlerItemOwnershipStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerItemOwnershipStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.rebuild()
                RepeatStatus.FINISHED
            })
            .build()
}
