package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleResultStatsAggregator
import com.imstargg.core.batch.job.statistics.task.DailyBattleStatsAggregationTask
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.infrastructure.db.core.statistics.QBrawlerBattleResultStatsEntity.Companion.brawlerBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity.Companion.statsResultBattleEntity
import jakarta.persistence.EntityManager
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
class BrawlerBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val em: EntityManager,
    private val aggregationTask: DailyBattleStatsAggregationTask,
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
                aggregationTask.aggregate(
                    em = em,
                    jobName = JOB_NAME,
                    date = dateJobParameter().date!!,
                    readPage = { queryFactory, date, lastId ->
                        queryFactory
                            .selectFrom(statsResultBattleEntity)
                            .where(
                                statsResultBattleEntity.battleDate.eq(date),
                                statsResultBattleEntity.id.gt(lastId),
                            ).orderBy(statsResultBattleEntity.id.asc())
                            .limit(BattleStatsConstants.READ_PAGE_SIZE)
                            .fetch()
                            .toList()
                    },
                    getId = { it.id!! },
                    aggregator = BrawlerBattleResultStatsAggregator(),
                    deleteExisting = { queryFactory, date ->
                        queryFactory
                            .delete(brawlerBattleResultStatsEntity)
                            .where(brawlerBattleResultStatsEntity.battleDate.eq(date))
                            .execute()
                    },
                )
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
