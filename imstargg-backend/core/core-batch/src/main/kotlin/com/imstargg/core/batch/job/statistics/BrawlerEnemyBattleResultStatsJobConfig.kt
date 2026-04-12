package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerEnemyBattleResultStatsAggregator
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity.Companion.statsResultBattleEntity
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrawlerEnemyBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
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
            .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
            .start(brawlerEnemyBattleResultStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerEnemyBattleResultStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                DailyBattleStatsTasklet(
                    entityManagerFactory = entityManagerFactory,
                    jobName = JOB_NAME,
                    dateSupplier = { dateJobParameter().date!! },
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
                    aggregatorFactory = { BrawlerEnemyBattleResultStatsAggregator() },
                    deleteExisting = { queryFactory, date ->
                        queryFactory
                            .delete(brawlerEnemyBattleResultStatsEntity)
                            .where(
                                brawlerEnemyBattleResultStatsEntity.periodType
                                    .eq(StatsPeriodType.DAILY),
                                brawlerEnemyBattleResultStatsEntity.battleDate.eq(date),
                            ).execute()
                    },
                ),
                txManager,
            ).build()
}
