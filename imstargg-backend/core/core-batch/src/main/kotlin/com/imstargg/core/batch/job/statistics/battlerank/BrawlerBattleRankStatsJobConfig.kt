package com.imstargg.core.batch.job.statistics.battlerank

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
class BrawlerBattleRankStatsJobConfig(
    private val jobRepository: JobRepository,
    private val task: BrawlerBattleRankStatsTask,
) {
    companion object {
        const val JOB_NAME = "brawlerBattleRankStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerBattleRankStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerBattleRankStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerBattleRankStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(brawlerBattleRankStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerBattleRankStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.aggregate(dateJobParameter().date!!)
                RepeatStatus.FINISHED
            })
            .build()
}
