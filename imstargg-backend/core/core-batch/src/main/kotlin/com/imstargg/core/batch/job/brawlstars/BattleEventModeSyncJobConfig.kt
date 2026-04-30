package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.job.brawlstars.task.BattleEventModeSyncTask
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
class BattleEventModeSyncJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val task: BattleEventModeSyncTask,
) {
    companion object {
        const val JOB_NAME = "battleEventModeSyncJob"
        const val STEP_NAME = "battleEventModeSyncStep"
    }

    @Bean(JOB_NAME)
    fun battleEventModeSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(battleEventModeSyncStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(STEP_NAME)
    fun battleEventModeSyncStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    task.sync()
                },
                txManager,
            ).build()
}
