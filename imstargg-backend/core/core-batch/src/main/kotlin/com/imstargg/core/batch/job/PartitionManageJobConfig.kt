package com.imstargg.core.batch.job

import com.imstargg.core.batch.support.parameter.DateJobParameter
import io.github.oshai.kotlinlogging.KotlinLogging
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import java.sql.Date
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Configuration
class PartitionManageJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val jdbcTemplate: JdbcTemplate,
) {
    companion object {
        const val JOB_NAME = "partitionManageJob"
        const val DATE_PARAMETER_NAME = "partitionManageJobDateJobParameter"
        private const val CREATE_STEP_NAME = "partitionCreateStep"
        private const val DROP_STEP_NAME = "partitionDropStep"
        private const val MONTH_CREATE_STEP_NAME = "monthPartitionCreateStep"
        private const val MONTH_DROP_STEP_NAME = "monthPartitionDropStep"
        private const val DAYS_AHEAD = 7

        /**
         * 원본 배틀(stats_*_battle) 파티션 보존 일수.
         * 주간 통계 잡의 최대 룩백([com.imstargg.core.enums.StatsWeekRange.currentAndPrevious] 기준
         * 최대 7일 + 실행일 보정 1일)을 커버해야 한다 — 가드: PartitionRetentionLookbackTest
         *
         * 45 는 패치 기준 통계 재설계(새 집계 구조) 가동 전까지 원본을 보존하기 위한 임시값이다(기존 10).
         * 새 집계 구조가 가동되면 다시 줄인다.
         */
        const val RETENTION_DAYS = 45

        /**
         * 조합 통계 3종(pair·trio·enemy)의 월 파티션. 삭제 경계는 API 최대 조회 범위 SINCE_LAST_MONTH 와 같다 —
         * 가드: MonthPartitionRetentionTest
         *
         * 이번 달에 더해 [MONTHS_AHEAD] 달 뒤까지 미리 만든다.
         */
        const val MONTHS_AHEAD = 1

        /**
         * 월 파티션 삭제 경계 — [date] 가 속한 달의 1일에서 한 달을 뺀 날짜(지난달 1일).
         * 이 날짜가 속한 달보다 이전 달의 파티션을 드롭한다.
         * SQL `drop_old_month_partitions` 의 `date_trunc('month', base_date) - interval '1 month'` 와 같은 경계다.
         * 이 경계는 API `SINCE_LAST_MONTH` 의 시작일(지난달 1일)과 같아서, API 가 읽을 수 있는 구간은 지워지지 않는다.
         */
        fun monthRetentionCutoff(date: LocalDate): LocalDate = date.withDayOfMonth(1).minusMonths(1)
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun partitionManageJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(partitionCreateStep())
            .next(partitionDropStep())
            .next(monthPartitionCreateStep())
            .next(monthPartitionDropStep())
            .build()

    @Bean(CREATE_STEP_NAME)
    @JobScope
    fun partitionCreateStep(): Step =
        StepBuilder(CREATE_STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                log.info { "파티션 생성 시작. date=$date, daysAhead=$DAYS_AHEAD" }
                jdbcTemplate.queryForObject(
                    "SELECT create_partitions(?, ?)",
                    Any::class.java,
                    Date.valueOf(date),
                    DAYS_AHEAD,
                )
                log.info { "파티션 생성 완료" }
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(DROP_STEP_NAME)
    @JobScope
    fun partitionDropStep(): Step =
        StepBuilder(DROP_STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                log.info { "오래된 파티션 삭제 시작. date=$date, retentionDays=$RETENTION_DAYS" }
                jdbcTemplate.queryForObject(
                    "SELECT drop_old_partitions(?, ?)",
                    Any::class.java,
                    Date.valueOf(date),
                    RETENTION_DAYS,
                )
                log.info { "오래된 파티션 삭제 완료" }
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(MONTH_CREATE_STEP_NAME)
    @JobScope
    fun monthPartitionCreateStep(): Step =
        StepBuilder(MONTH_CREATE_STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                log.info { "월 파티션 생성 시작. date=$date, monthsAhead=$MONTHS_AHEAD" }
                jdbcTemplate.queryForObject(
                    "SELECT create_month_partitions(?, ?)",
                    Any::class.java,
                    Date.valueOf(date),
                    MONTHS_AHEAD,
                )
                log.info { "월 파티션 생성 완료" }
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(MONTH_DROP_STEP_NAME)
    @JobScope
    fun monthPartitionDropStep(): Step =
        StepBuilder(MONTH_DROP_STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                log.info { "월 파티션 삭제 시작. date=$date, cutoffMonth=${monthRetentionCutoff(date)}" }
                jdbcTemplate.queryForObject(
                    "SELECT drop_old_month_partitions(?)",
                    Any::class.java,
                    Date.valueOf(date),
                )
                log.info { "월 파티션 삭제 완료" }
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
