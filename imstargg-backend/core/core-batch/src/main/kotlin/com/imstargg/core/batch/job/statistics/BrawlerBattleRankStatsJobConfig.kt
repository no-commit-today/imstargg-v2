package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleRankStatsAggregator
import com.imstargg.core.batch.job.statistics.task.DailyBattleStatsAggregationTask
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.infrastructure.db.core.statistics.QBrawlerBattleRankStatsEntity.Companion.brawlerBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity.Companion.statsRankBattleEntity
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
class BrawlerBattleRankStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val em: EntityManager,
    private val aggregationTask: DailyBattleStatsAggregationTask,
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
                aggregationTask.aggregate(
                    em = em,
                    jobName = JOB_NAME,
                    date = dateJobParameter().date!!,
                    readPage = { queryFactory, date, lastId ->
                        queryFactory
                            .selectFrom(statsRankBattleEntity)
                            .where(
                                statsRankBattleEntity.battleDate.eq(date),
                                statsRankBattleEntity.id.gt(lastId),
                            ).orderBy(statsRankBattleEntity.id.asc())
                            .limit(BattleStatsConstants.READ_PAGE_SIZE)
                            .fetch()
                            .toList()
                    },
                    getId = { it.id!! },
                    aggregator = BrawlerBattleRankStatsAggregator(),
                    deleteExisting = { queryFactory, date ->
                        queryFactory
                            .delete(brawlerBattleRankStatsEntity)
                            .where(brawlerBattleRankStatsEntity.battleDate.eq(date))
                            .execute()
                    },
                )
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
