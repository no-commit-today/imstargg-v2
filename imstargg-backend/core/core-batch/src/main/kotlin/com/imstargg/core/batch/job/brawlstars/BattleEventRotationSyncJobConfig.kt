package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.job.brawlstars.task.BattleEventRotationSyncTask
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BattleEventRotationSyncJobConfig(
    private val jobRepository: JobRepository,
    private val task: BattleEventRotationSyncTask,
) {
    companion object {
        const val JOB_NAME = "battleEventRotationSyncJob"
        const val STEP_NAME = "battleEventRotationSyncStep"
    }

    @Bean(JOB_NAME)
    fun battleEventRotationSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(battleEventRotationSyncStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(STEP_NAME)
    fun battleEventRotationSyncStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    task.sync()
                },
            ).build()
}
