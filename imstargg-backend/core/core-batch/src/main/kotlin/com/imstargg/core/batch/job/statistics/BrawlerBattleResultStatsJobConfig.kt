package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.support.parameter.DateJobParameter
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BrawlerBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val task: BrawlerBattleResultStatsTask,
) {
    companion object {
        const val JOB_NAME = "brawlerBattleResultStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerBattleResultStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerBattleResultStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerBattleResultStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(brawlerBattleResultStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerBattleResultStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.aggregate(dateJobParameter().date!!)
                RepeatStatus.FINISHED
            })
            .build()
}
