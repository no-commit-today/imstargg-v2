package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.job.brawlstars.task.BrawlStarsApiSpecTrackTask
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
class BrawlStarsApiSpecTrackJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val task: BrawlStarsApiSpecTrackTask,
) {
    companion object {
        const val JOB_NAME = "brawlStarsApiSpecTrackJob"
        const val STEP_NAME = "brawlStarsApiSpecTrackStep"
    }

    @Bean(JOB_NAME)
    fun brawlStarsApiSpecTrackJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(brawlStarsApiSpecTrackStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(STEP_NAME)
    fun brawlStarsApiSpecTrackStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    task.track()
                },
                txManager,
            ).build()
}
