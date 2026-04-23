package com.imstargg.core.batch.job

import com.imstargg.core.batch.support.parameter.DateJobParameter
import org.slf4j.LoggerFactory
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

@Configuration
class PartitionManageJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val jdbcTemplate: JdbcTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val JOB_NAME = "partitionManageJob"
        const val DATE_PARAMETER_NAME = "partitionManageJobDateJobParameter"
        private const val CREATE_STEP_NAME = "partitionCreateStep"
        private const val DROP_STEP_NAME = "partitionDropStep"
        private const val DAYS_AHEAD = 7
        private const val RETENTION_DAYS = 10
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
                    arrayOf("timestamp", "manualRunAt", "retry"),
                ),
            ).start(partitionCreateStep())
            .next(partitionDropStep())
            .build()

    @Bean(CREATE_STEP_NAME)
    @JobScope
    fun partitionCreateStep(): Step =
        StepBuilder(CREATE_STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                log.info("파티션 생성 시작. date={}, daysAhead={}", date, DAYS_AHEAD)
                jdbcTemplate.update(
                    "SELECT create_partitions(?, ?)",
                    Date.valueOf(date),
                    DAYS_AHEAD,
                )
                log.info("파티션 생성 완료")
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(DROP_STEP_NAME)
    @JobScope
    fun partitionDropStep(): Step =
        StepBuilder(DROP_STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                log.info("오래된 파티션 삭제 시작. date={}, retentionDays={}", date, RETENTION_DAYS)
                jdbcTemplate.update(
                    "SELECT drop_old_partitions(?, ?)",
                    Date.valueOf(date),
                    RETENTION_DAYS,
                )
                log.info("오래된 파티션 삭제 완료")
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
