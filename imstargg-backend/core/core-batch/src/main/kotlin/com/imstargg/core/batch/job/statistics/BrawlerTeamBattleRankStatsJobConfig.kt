package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerTeamBattleRankStatsAggregator
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsBrawlerEntity.Companion.brawlerTeamBattleRankStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsEntity.Companion.brawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity.Companion.statsRankBattleEntity
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
class BrawlerTeamBattleRankStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
) {
    companion object {
        const val JOB_NAME = "brawlerTeamBattleRankStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerTeamBattleRankStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerTeamBattleRankStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerTeamBattleRankStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
            .start(brawlerTeamBattleRankStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerTeamBattleRankStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                PerEventDailyBattleStatsTasklet(
                    entityManagerFactory = entityManagerFactory,
                    jobName = JOB_NAME,
                    dateSupplier = { dateJobParameter().date!! },
                    readDistinctEventIds = { queryFactory, date ->
                        queryFactory
                            .select(statsRankBattleEntity.eventBrawlStarsId)
                            .distinct()
                            .from(statsRankBattleEntity)
                            .where(statsRankBattleEntity.battleDate.eq(date))
                            .fetch()
                            .toList()
                    },
                    readPage = { queryFactory, date, lastId, eventBrawlStarsId ->
                        queryFactory
                            .selectFrom(statsRankBattleEntity)
                            .where(
                                statsRankBattleEntity.battleDate.eq(date),
                                statsRankBattleEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                                statsRankBattleEntity.id.gt(lastId),
                            ).orderBy(statsRankBattleEntity.id.asc())
                            .limit(5000)
                            .fetch()
                            .toList()
                    },
                    getId = { it.id!! },
                    aggregatorFactory = { BrawlerTeamBattleRankStatsAggregator() },
                    deleteExisting = { queryFactory, date, eventBrawlStarsId ->
                        queryFactory
                            .delete(brawlerTeamBattleRankStatsBrawlerEntity)
                            .where(
                                brawlerTeamBattleRankStatsBrawlerEntity.stats.id.`in`(
                                    JPAExpressions
                                        .select(brawlerTeamBattleRankStatsEntity.id)
                                        .from(brawlerTeamBattleRankStatsEntity)
                                        .where(
                                            brawlerTeamBattleRankStatsEntity.periodType
                                                .eq(StatsPeriodType.DAILY),
                                            brawlerTeamBattleRankStatsEntity.startBattleDate
                                                .eq(date),
                                            brawlerTeamBattleRankStatsEntity.eventBrawlStarsId
                                                .eq(eventBrawlStarsId),
                                        ),
                                ),
                            ).execute()
                        queryFactory
                            .delete(brawlerTeamBattleRankStatsEntity)
                            .where(
                                brawlerTeamBattleRankStatsEntity.periodType
                                    .eq(StatsPeriodType.DAILY),
                                brawlerTeamBattleRankStatsEntity.startBattleDate.eq(date),
                                brawlerTeamBattleRankStatsEntity.eventBrawlStarsId
                                    .eq(eventBrawlStarsId),
                            ).execute()
                    },
                ),
                txManager,
            ).build()
}
