package com.imstargg.core.batch.job

import com.imstargg.core.batch.support.parameter.YearMonthJobParameter
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
class MonthlyStatsAggregationJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val periodStatsAggregationHelper: PeriodStatsAggregationHelper,
) {
    companion object {
        const val JOB_NAME = "monthlyStatsAggregationJob"
        const val YEAR_MONTH_PARAMETER_NAME = "monthlyStatsAggregationJobYearMonthJobParameter"
        private const val STEP_TEAM_RESULT = "monthlyStatsAggregationTeamResultStep"
        private const val STEP_ENEMY_RESULT = "monthlyStatsAggregationEnemyResultStep"
        private const val STEP_TEAM_RANK = "monthlyStatsAggregationTeamRankStep"
    }

    @Bean(YEAR_MONTH_PARAMETER_NAME)
    @JobScope
    fun yearMonthJobParameter() = YearMonthJobParameter()

    @Bean(JOB_NAME)
    fun monthlyStatsAggregationJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(
                DefaultJobParametersValidator(arrayOf(YearMonthJobParameter.KEY), arrayOf()),
            ).start(monthlyStatsAggregationTeamResultStep())
            .next(monthlyStatsAggregationEnemyResultStep())
            .next(monthlyStatsAggregationTeamRankStep())
            .build()

    @Bean(STEP_TEAM_RESULT)
    @JobScope
    fun monthlyStatsAggregationTeamResultStep(): Step =
        StepBuilder(STEP_TEAM_RESULT, jobRepository)
            .tasklet({ _, _ ->
                val yearMonth = yearMonthJobParameter().yearMonth!!
                periodStatsAggregationHelper.aggregateTeamResult(
                    sourcePeriodType = StatsPeriodType.WEEKLY,
                    targetPeriodType = StatsPeriodType.MONTHLY,
                    startDate = yearMonth.atDay(1),
                    endDate = yearMonth.atEndOfMonth(),
                    jobName = JOB_NAME,
                )
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(STEP_ENEMY_RESULT)
    @JobScope
    fun monthlyStatsAggregationEnemyResultStep(): Step =
        StepBuilder(STEP_ENEMY_RESULT, jobRepository)
            .tasklet({ _, _ ->
                val yearMonth = yearMonthJobParameter().yearMonth!!
                periodStatsAggregationHelper.aggregateEnemyResult(
                    sourcePeriodType = StatsPeriodType.WEEKLY,
                    targetPeriodType = StatsPeriodType.MONTHLY,
                    startDate = yearMonth.atDay(1),
                    endDate = yearMonth.atEndOfMonth(),
                    jobName = JOB_NAME,
                )
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(STEP_TEAM_RANK)
    @JobScope
    fun monthlyStatsAggregationTeamRankStep(): Step =
        StepBuilder(STEP_TEAM_RANK, jobRepository)
            .tasklet({ _, _ ->
                val yearMonth = yearMonthJobParameter().yearMonth!!
                periodStatsAggregationHelper.aggregateTeamRank(
                    sourcePeriodType = StatsPeriodType.WEEKLY,
                    targetPeriodType = StatsPeriodType.MONTHLY,
                    startDate = yearMonth.atDay(1),
                    endDate = yearMonth.atEndOfMonth(),
                    jobName = JOB_NAME,
                )
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
