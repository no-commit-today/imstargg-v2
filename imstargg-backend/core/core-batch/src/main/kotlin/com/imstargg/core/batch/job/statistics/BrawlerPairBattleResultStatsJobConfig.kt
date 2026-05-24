package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsWeekRange
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
class BrawlerPairBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val task: BrawlerPairBattleResultStatsTask,
) {
    companion object {
        const val JOB_NAME = "brawlerPairBattleResultStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerPairBattleResultStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerPairBattleResultStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerPairBattleResultStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(brawlerPairBattleResultStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerPairBattleResultStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.aggregate(StatsWeekRange.currentAndPrevious(dateJobParameter().date!!))
                RepeatStatus.FINISHED
            })
            .build()
}
