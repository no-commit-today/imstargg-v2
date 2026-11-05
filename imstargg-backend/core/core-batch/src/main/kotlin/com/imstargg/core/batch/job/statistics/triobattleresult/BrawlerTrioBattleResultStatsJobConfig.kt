package com.imstargg.core.batch.job.statistics.triobattleresult

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
class BrawlerTrioBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val task: BrawlerTrioBattleResultStatsTask,
) {
    companion object {
        const val JOB_NAME = "brawlerTrioBattleResultStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerTrioBattleResultStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerTrioBattleResultStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerTrioBattleResultStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(brawlerTrioBattleResultStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerTrioBattleResultStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.aggregate(StatsWeekRange.currentAndPrevious(dateJobParameter().date!!))
                RepeatStatus.FINISHED
            })
            .build()
}
