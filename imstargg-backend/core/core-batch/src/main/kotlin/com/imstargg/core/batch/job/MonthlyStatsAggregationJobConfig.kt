package com.imstargg.core.batch.job

import com.imstargg.core.batch.support.parameter.YearMonthJobParameter
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsBrawlerEntity.Companion.brawlerTeamBattleRankStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsEntity.Companion.brawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsBrawlerEntity.Companion.brawlerTeamBattleResultStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsEntity.Companion.brawlerTeamBattleResultStatsEntity
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
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
    private val entityManagerFactory: EntityManagerFactory,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val JOB_NAME = "monthlyStatsAggregationJob"
        const val YEAR_MONTH_PARAMETER_NAME = "monthlyStatsAggregationJobYearMonthJobParameter"
        private const val STEP_TEAM_RESULT = "monthlyStatsAggregationTeamResultStep"
        private const val STEP_ENEMY_RESULT = "monthlyStatsAggregationEnemyResultStep"
        private const val STEP_TEAM_RANK = "monthlyStatsAggregationTeamRankStep"
        private const val BATCH_SIZE = 1000
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
            )
            .start(monthlyStatsAggregationTeamResultStep())
            .next(monthlyStatsAggregationEnemyResultStep())
            .next(monthlyStatsAggregationTeamRankStep())
            .build()

    @Bean(STEP_TEAM_RESULT)
    @JobScope
    fun monthlyStatsAggregationTeamResultStep(): Step =
        StepBuilder(STEP_TEAM_RESULT, jobRepository)
            .tasklet({ _, _ ->
                val yearMonth = yearMonthJobParameter().yearMonth!!
                val startDate = yearMonth.atDay(1)
                val endDate = yearMonth.atEndOfMonth()
                val entityManager = entityManagerFactory.createEntityManager()

                try {
                    val queryFactory = JPAQueryFactory(entityManager)

                    val eventIds =
                        queryFactory
                            .select(brawlerTeamBattleResultStatsEntity.eventBrawlStarsId)
                            .distinct()
                            .from(brawlerTeamBattleResultStatsEntity)
                            .where(
                                brawlerTeamBattleResultStatsEntity.periodType
                                    .eq(StatsPeriodType.WEEKLY),
                                brawlerTeamBattleResultStatsEntity.startBattleDate
                                    .between(startDate, endDate),
                            ).fetch()
                            .toList()

                    var totalAggregated = 0L
                    var totalDeleted = 0L

                    for (eventId in eventIds) {
                        val weeklyEntities =
                            queryFactory
                                .selectFrom(brawlerTeamBattleResultStatsEntity)
                                .distinct()
                                .leftJoin(
                                    brawlerTeamBattleResultStatsEntity.brawlers,
                                    brawlerTeamBattleResultStatsBrawlerEntity,
                                ).fetchJoin()
                                .where(
                                    brawlerTeamBattleResultStatsEntity.periodType
                                        .eq(StatsPeriodType.WEEKLY),
                                    brawlerTeamBattleResultStatsEntity.startBattleDate
                                        .between(startDate, endDate),
                                    brawlerTeamBattleResultStatsEntity.eventBrawlStarsId
                                        .eq(eventId),
                                ).fetch()
                                .toList()

                        val aggregated =
                            weeklyEntities.groupBy {
                                Pair(it.tierRange, it.brawlerBrawlStarsIds)
                            }

                        val monthlyEntities =
                            aggregated.map { (key, group) ->
                                val totalResult =
                                    group.map { it.resultCount }.reduce { acc, r -> acc + r }
                                BrawlerTeamBattleResultStatsEntity(
                                    eventBrawlStarsId = eventId,
                                    periodType = StatsPeriodType.MONTHLY,
                                    tierRange = key.first,
                                    startBattleDate = startDate,
                                    endBattleDate = endDate,
                                    teamSize = key.second.size,
                                    resultCount = totalResult,
                                    initialBrawlerBrawlStarsIds = key.second,
                                )
                            }

                        monthlyEntities.chunked(BATCH_SIZE).forEach { batch ->
                            entityManager.transaction.begin()
                            batch.forEach { entityManager.persist(it) }
                            entityManager.transaction.commit()
                            entityManager.clear()
                        }
                        totalAggregated += monthlyEntities.size

                        entityManager.transaction.begin()
                        queryFactory
                            .delete(brawlerTeamBattleResultStatsBrawlerEntity)
                            .where(
                                brawlerTeamBattleResultStatsBrawlerEntity.stats.id.`in`(
                                    JPAExpressions
                                        .select(brawlerTeamBattleResultStatsEntity.id)
                                        .from(brawlerTeamBattleResultStatsEntity)
                                        .where(
                                            brawlerTeamBattleResultStatsEntity.periodType
                                                .eq(StatsPeriodType.WEEKLY),
                                            brawlerTeamBattleResultStatsEntity.startBattleDate
                                                .between(startDate, endDate),
                                            brawlerTeamBattleResultStatsEntity.eventBrawlStarsId
                                                .eq(eventId),
                                        ),
                                ),
                            ).execute()
                        val deleted =
                            queryFactory
                                .delete(brawlerTeamBattleResultStatsEntity)
                                .where(
                                    brawlerTeamBattleResultStatsEntity.periodType
                                        .eq(StatsPeriodType.WEEKLY),
                                    brawlerTeamBattleResultStatsEntity.startBattleDate
                                        .between(startDate, endDate),
                                    brawlerTeamBattleResultStatsEntity.eventBrawlStarsId
                                        .eq(eventId),
                                ).execute()
                        entityManager.transaction.commit()
                        totalDeleted += deleted
                    }

                    log.info(
                        "[{}] Team Result 월간 집계 완료 - 대상월: {}, 집계: {}건, 삭제: {}건",
                        JOB_NAME,
                        yearMonth,
                        totalAggregated,
                        totalDeleted,
                    )
                } finally {
                    entityManager.close()
                }
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(STEP_ENEMY_RESULT)
    @JobScope
    fun monthlyStatsAggregationEnemyResultStep(): Step =
        StepBuilder(STEP_ENEMY_RESULT, jobRepository)
            .tasklet({ _, _ ->
                val yearMonth = yearMonthJobParameter().yearMonth!!
                val startDate = yearMonth.atDay(1)
                val endDate = yearMonth.atEndOfMonth()
                val entityManager = entityManagerFactory.createEntityManager()

                try {
                    val queryFactory = JPAQueryFactory(entityManager)

                    val eventIds =
                        queryFactory
                            .select(brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId)
                            .distinct()
                            .from(brawlerEnemyBattleResultStatsEntity)
                            .where(
                                brawlerEnemyBattleResultStatsEntity.periodType
                                    .eq(StatsPeriodType.WEEKLY),
                                brawlerEnemyBattleResultStatsEntity.startBattleDate
                                    .between(startDate, endDate),
                            ).fetch()
                            .toList()

                    var totalAggregated = 0L
                    var totalDeleted = 0L

                    for (eventId in eventIds) {
                        val weeklyEntities =
                            queryFactory
                                .selectFrom(brawlerEnemyBattleResultStatsEntity)
                                .where(
                                    brawlerEnemyBattleResultStatsEntity.periodType
                                        .eq(StatsPeriodType.WEEKLY),
                                    brawlerEnemyBattleResultStatsEntity.startBattleDate
                                        .between(startDate, endDate),
                                    brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId
                                        .eq(eventId),
                                ).fetch()
                                .toList()

                        val aggregated =
                            weeklyEntities.groupBy {
                                Triple(
                                    it.tierRange,
                                    it.brawlerBrawlStarsId,
                                    it.enemyBrawlerBrawlStarsId,
                                )
                            }

                        val monthlyEntities =
                            aggregated.map { (key, group) ->
                                val totalResult =
                                    group.map { it.resultCount }.reduce { acc, r -> acc + r }
                                BrawlerEnemyBattleResultStatsEntity(
                                    eventBrawlStarsId = eventId,
                                    periodType = StatsPeriodType.MONTHLY,
                                    tierRange = key.first,
                                    startBattleDate = startDate,
                                    endBattleDate = endDate,
                                    brawlerBrawlStarsId = key.second,
                                    enemyBrawlerBrawlStarsId = key.third,
                                    resultCount = totalResult,
                                )
                            }

                        monthlyEntities.chunked(BATCH_SIZE).forEach { batch ->
                            entityManager.transaction.begin()
                            batch.forEach { entityManager.persist(it) }
                            entityManager.transaction.commit()
                            entityManager.clear()
                        }
                        totalAggregated += monthlyEntities.size

                        entityManager.transaction.begin()
                        val deleted =
                            queryFactory
                                .delete(brawlerEnemyBattleResultStatsEntity)
                                .where(
                                    brawlerEnemyBattleResultStatsEntity.periodType
                                        .eq(StatsPeriodType.WEEKLY),
                                    brawlerEnemyBattleResultStatsEntity.startBattleDate
                                        .between(startDate, endDate),
                                    brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId
                                        .eq(eventId),
                                ).execute()
                        entityManager.transaction.commit()
                        totalDeleted += deleted
                    }

                    log.info(
                        "[{}] Enemy Result 월간 집계 완료 - 대상월: {}, 집계: {}건, 삭제: {}건",
                        JOB_NAME,
                        yearMonth,
                        totalAggregated,
                        totalDeleted,
                    )
                } finally {
                    entityManager.close()
                }
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(STEP_TEAM_RANK)
    @JobScope
    fun monthlyStatsAggregationTeamRankStep(): Step =
        StepBuilder(STEP_TEAM_RANK, jobRepository)
            .tasklet({ _, _ ->
                val yearMonth = yearMonthJobParameter().yearMonth!!
                val startDate = yearMonth.atDay(1)
                val endDate = yearMonth.atEndOfMonth()
                val entityManager = entityManagerFactory.createEntityManager()

                try {
                    val queryFactory = JPAQueryFactory(entityManager)

                    val eventIds =
                        queryFactory
                            .select(brawlerTeamBattleRankStatsEntity.eventBrawlStarsId)
                            .distinct()
                            .from(brawlerTeamBattleRankStatsEntity)
                            .where(
                                brawlerTeamBattleRankStatsEntity.periodType
                                    .eq(StatsPeriodType.WEEKLY),
                                brawlerTeamBattleRankStatsEntity.startBattleDate
                                    .between(startDate, endDate),
                            ).fetch()
                            .toList()

                    var totalAggregated = 0L
                    var totalDeleted = 0L

                    for (eventId in eventIds) {
                        val weeklyEntities =
                            queryFactory
                                .selectFrom(brawlerTeamBattleRankStatsEntity)
                                .distinct()
                                .leftJoin(
                                    brawlerTeamBattleRankStatsEntity.brawlers,
                                    brawlerTeamBattleRankStatsBrawlerEntity,
                                ).fetchJoin()
                                .where(
                                    brawlerTeamBattleRankStatsEntity.periodType
                                        .eq(StatsPeriodType.WEEKLY),
                                    brawlerTeamBattleRankStatsEntity.startBattleDate
                                        .between(startDate, endDate),
                                    brawlerTeamBattleRankStatsEntity.eventBrawlStarsId
                                        .eq(eventId),
                                ).fetch()
                                .toList()

                        val aggregated =
                            weeklyEntities.groupBy {
                                Pair(it.tierRange, it.brawlerBrawlStarsIds)
                            }

                        val monthlyEntities =
                            aggregated.map { (key, group) ->
                                val combinedRanks =
                                    group.map { it.rankCounts }.reduce { acc, r -> acc + r }
                                BrawlerTeamBattleRankStatsEntity(
                                    eventBrawlStarsId = eventId,
                                    periodType = StatsPeriodType.MONTHLY,
                                    tierRange = key.first,
                                    startBattleDate = startDate,
                                    endBattleDate = endDate,
                                    teamSize = key.second.size,
                                    rankCounts = combinedRanks,
                                    initialBrawlerBrawlStarsIds = key.second,
                                )
                            }

                        monthlyEntities.chunked(BATCH_SIZE).forEach { batch ->
                            entityManager.transaction.begin()
                            batch.forEach { entityManager.persist(it) }
                            entityManager.transaction.commit()
                            entityManager.clear()
                        }
                        totalAggregated += monthlyEntities.size

                        entityManager.transaction.begin()
                        queryFactory
                            .delete(brawlerTeamBattleRankStatsBrawlerEntity)
                            .where(
                                brawlerTeamBattleRankStatsBrawlerEntity.stats.id.`in`(
                                    JPAExpressions
                                        .select(brawlerTeamBattleRankStatsEntity.id)
                                        .from(brawlerTeamBattleRankStatsEntity)
                                        .where(
                                            brawlerTeamBattleRankStatsEntity.periodType
                                                .eq(StatsPeriodType.WEEKLY),
                                            brawlerTeamBattleRankStatsEntity.startBattleDate
                                                .between(startDate, endDate),
                                            brawlerTeamBattleRankStatsEntity.eventBrawlStarsId
                                                .eq(eventId),
                                        ),
                                ),
                            ).execute()
                        val deleted =
                            queryFactory
                                .delete(brawlerTeamBattleRankStatsEntity)
                                .where(
                                    brawlerTeamBattleRankStatsEntity.periodType
                                        .eq(StatsPeriodType.WEEKLY),
                                    brawlerTeamBattleRankStatsEntity.startBattleDate
                                        .between(startDate, endDate),
                                    brawlerTeamBattleRankStatsEntity.eventBrawlStarsId
                                        .eq(eventId),
                                ).execute()
                        entityManager.transaction.commit()
                        totalDeleted += deleted
                    }

                    log.info(
                        "[{}] Team Rank 월간 집계 완료 - 대상월: {}, 집계: {}건, 삭제: {}건",
                        JOB_NAME,
                        yearMonth,
                        totalAggregated,
                        totalDeleted,
                    )
                } finally {
                    entityManager.close()
                }
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
