package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerTeamBattleResultStatsAggregator
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsBrawlerEntity.Companion.brawlerTeamBattleResultStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsEntity.Companion.brawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity.Companion.statsResultBattleEntity
import com.querydsl.jpa.JPAExpressions
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrawlerTeamBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
) {
    companion object {
        const val JOB_NAME = "brawlerTeamBattleResultStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerTeamBattleResultStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerTeamBattleResultStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerTeamBattleResultStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
            .start(brawlerTeamBattleResultStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerTeamBattleResultStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                PerEventDailyBattleStatsTasklet(
                    entityManagerFactory = entityManagerFactory,
                    jobName = JOB_NAME,
                    dateSupplier = { dateJobParameter().date!! },
                    readDistinctEventIds = { queryFactory, date ->
                        queryFactory
                            .select(statsResultBattleEntity.eventBrawlStarsId)
                            .distinct()
                            .from(statsResultBattleEntity)
                            .where(statsResultBattleEntity.battleDate.eq(date))
                            .fetch()
                            .toList()
                    },
                    readPage = { queryFactory, date, lastId, eventBrawlStarsId ->
                        queryFactory
                            .selectFrom(statsResultBattleEntity)
                            .where(
                                statsResultBattleEntity.battleDate.eq(date),
                                statsResultBattleEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                                statsResultBattleEntity.id.gt(lastId),
                            ).orderBy(statsResultBattleEntity.id.asc())
                            .limit(5000)
                            .fetch()
                            .toList()
                    },
                    getId = { it.id!! },
                    aggregatorFactory = { BrawlerTeamBattleResultStatsAggregator() },
                    deleteExisting = { queryFactory, date, eventBrawlStarsId ->
                        queryFactory
                            .delete(brawlerTeamBattleResultStatsBrawlerEntity)
                            .where(
                                brawlerTeamBattleResultStatsBrawlerEntity.stats.id.`in`(
                                    JPAExpressions
                                        .select(brawlerTeamBattleResultStatsEntity.id)
                                        .from(brawlerTeamBattleResultStatsEntity)
                                        .where(
                                            brawlerTeamBattleResultStatsEntity.periodType
                                                .eq(StatsPeriodType.DAILY),
                                            brawlerTeamBattleResultStatsEntity.startBattleDate
                                                .eq(date),
                                            brawlerTeamBattleResultStatsEntity.eventBrawlStarsId
                                                .eq(eventBrawlStarsId),
                                        ),
                                ),
                            ).execute()
                        queryFactory
                            .delete(brawlerTeamBattleResultStatsEntity)
                            .where(
                                brawlerTeamBattleResultStatsEntity.periodType
                                    .eq(StatsPeriodType.DAILY),
                                brawlerTeamBattleResultStatsEntity.startBattleDate.eq(date),
                                brawlerTeamBattleResultStatsEntity.eventBrawlStarsId
                                    .eq(eventBrawlStarsId),
                            ).execute()
                    },
                ),
                txManager,
            ).build()
}
