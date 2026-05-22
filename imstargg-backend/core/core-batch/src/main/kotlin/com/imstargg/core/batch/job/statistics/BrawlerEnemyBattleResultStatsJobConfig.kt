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
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrawlerEnemyBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val task: BrawlerEnemyBattleResultStatsTask,
) {
    companion object {
        const val JOB_NAME = "brawlerEnemyBattleResultStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerEnemyBattleResultStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerEnemyBattleResultStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerEnemyBattleResultStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(brawlerEnemyBattleResultStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerEnemyBattleResultStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                task.aggregate(StatsWeekRange.currentAndPrevious(dateJobParameter().date!!))
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
