package com.imstargg.core.batch.job

import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsPeriodType
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class WeeklyStatsAggregationJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val periodStatsAggregationHelper: PeriodStatsAggregationHelper,
) {
    companion object {
        const val JOB_NAME = "weeklyStatsAggregationJob"
        const val DATE_PARAMETER_NAME = "weeklyStatsAggregationJobDateJobParameter"
        private const val STEP_TEAM_RESULT = "weeklyStatsAggregationTeamResultStep"
        private const val STEP_TEAM_RANK = "weeklyStatsAggregationTeamRankStep"
        private const val STEP_ENEMY_RESULT = "weeklyStatsAggregationEnemyResultStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun weeklyStatsAggregationJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
            .start(weeklyStatsAggregationTeamResultStep())
            .next(weeklyStatsAggregationTeamRankStep())
            .next(weeklyStatsAggregationEnemyResultStep())
            .build()

    @Bean(STEP_TEAM_RESULT)
    @JobScope
    fun weeklyStatsAggregationTeamResultStep(): Step =
        StepBuilder(STEP_TEAM_RESULT, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                periodStatsAggregationHelper.aggregateTeamResult(
                    sourcePeriodType = StatsPeriodType.DAILY,
                    targetPeriodType = StatsPeriodType.WEEKLY,
                    startDate = date,
                    endDate = date.plusDays(6),
                    jobName = JOB_NAME,
                )
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(STEP_TEAM_RANK)
    @JobScope
    fun weeklyStatsAggregationTeamRankStep(): Step =
        StepBuilder(STEP_TEAM_RANK, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                periodStatsAggregationHelper.aggregateTeamRank(
                    sourcePeriodType = StatsPeriodType.DAILY,
                    targetPeriodType = StatsPeriodType.WEEKLY,
                    startDate = date,
                    endDate = date.plusDays(6),
                    jobName = JOB_NAME,
                )
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(STEP_ENEMY_RESULT)
    @JobScope
    fun weeklyStatsAggregationEnemyResultStep(): Step =
        StepBuilder(STEP_ENEMY_RESULT, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                periodStatsAggregationHelper.aggregateEnemyResult(
                    sourcePeriodType = StatsPeriodType.DAILY,
                    targetPeriodType = StatsPeriodType.WEEKLY,
                    startDate = date,
                    endDate = date.plusDays(6),
                    jobName = JOB_NAME,
                )
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
