package com.imstargg.core.batch.job

import com.imstargg.core.batch.support.parameter.DateJobParameter
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
class WeeklyStatsAggregationJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val JOB_NAME = "weeklyStatsAggregationJob"
        const val DATE_PARAMETER_NAME = "weeklyStatsAggregationJobDateJobParameter"
        private const val STEP_TEAM_RESULT = "weeklyStatsAggregationTeamResultStep"
        private const val STEP_TEAM_RANK = "weeklyStatsAggregationTeamRankStep"
        private const val STEP_ENEMY_RESULT = "weeklyStatsAggregationEnemyResultStep"
        private const val BATCH_SIZE = 1000
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
                val startDate = date
                val endDate = date.plusDays(6)
                aggregateTeamResult(startDate, endDate)
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(STEP_TEAM_RANK)
    @JobScope
    fun weeklyStatsAggregationTeamRankStep(): Step =
        StepBuilder(STEP_TEAM_RANK, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                val startDate = date
                val endDate = date.plusDays(6)
                aggregateTeamRank(startDate, endDate)
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(STEP_ENEMY_RESULT)
    @JobScope
    fun weeklyStatsAggregationEnemyResultStep(): Step =
        StepBuilder(STEP_ENEMY_RESULT, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                val startDate = date
                val endDate = date.plusDays(6)
                aggregateEnemyResult(startDate, endDate)
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    private fun aggregateTeamResult(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
    ) {
        val entityManager = entityManagerFactory.createEntityManager()

        try {
            val queryFactory = JPAQueryFactory(entityManager)

            val dailyEntities =
                queryFactory
                    .selectFrom(brawlerTeamBattleResultStatsEntity)
                    .distinct()
                    .leftJoin(
                        brawlerTeamBattleResultStatsEntity.brawlers,
                        brawlerTeamBattleResultStatsBrawlerEntity,
                    ).fetchJoin()
                    .where(
                        brawlerTeamBattleResultStatsEntity.periodType.eq(StatsPeriodType.DAILY),
                        brawlerTeamBattleResultStatsEntity.startBattleDate
                            .between(startDate, endDate),
                    ).fetch()
                    .toList()

            val aggregated =
                dailyEntities.groupBy {
                    Triple(it.eventBrawlStarsId, it.tierRange, it.brawlerBrawlStarsIds)
                }

            val weeklyEntities =
                aggregated.map { (key, group) ->
                    val totalResult = group.map { it.resultCount }.reduce { acc, r -> acc + r }
                    BrawlerTeamBattleResultStatsEntity(
                        eventBrawlStarsId = key.first,
                        periodType = StatsPeriodType.WEEKLY,
                        tierRange = key.second,
                        startBattleDate = startDate,
                        endBattleDate = endDate,
                        teamSize = key.third.size,
                        resultCount = totalResult,
                        initialBrawlerBrawlStarsIds = key.third,
                    )
                }

            weeklyEntities.chunked(BATCH_SIZE).forEach { batch ->
                entityManager.transaction.begin()
                batch.forEach { entityManager.persist(it) }
                entityManager.transaction.commit()
                entityManager.clear()
            }

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
                                    .eq(StatsPeriodType.DAILY),
                                brawlerTeamBattleResultStatsEntity.startBattleDate
                                    .between(startDate, endDate),
                            ),
                    ),
                ).execute()
            queryFactory
                .delete(brawlerTeamBattleResultStatsEntity)
                .where(
                    brawlerTeamBattleResultStatsEntity.periodType.eq(StatsPeriodType.DAILY),
                    brawlerTeamBattleResultStatsEntity.startBattleDate
                        .between(startDate, endDate),
                ).execute()
            entityManager.transaction.commit()

            log.info(
                "[{}] Team Result 주간 집계 완료 - 기간: {} ~ {}, 집계: {}건, 삭제: {}건",
                JOB_NAME,
                startDate,
                endDate,
                weeklyEntities.size,
                dailyEntities.size,
            )
        } finally {
            entityManager.close()
        }
    }

    private fun aggregateTeamRank(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
    ) {
        val entityManager = entityManagerFactory.createEntityManager()

        try {
            val queryFactory = JPAQueryFactory(entityManager)

            val dailyEntities =
                queryFactory
                    .selectFrom(brawlerTeamBattleRankStatsEntity)
                    .distinct()
                    .leftJoin(
                        brawlerTeamBattleRankStatsEntity.brawlers,
                        brawlerTeamBattleRankStatsBrawlerEntity,
                    ).fetchJoin()
                    .where(
                        brawlerTeamBattleRankStatsEntity.periodType.eq(StatsPeriodType.DAILY),
                        brawlerTeamBattleRankStatsEntity.startBattleDate
                            .between(startDate, endDate),
                    ).fetch()
                    .toList()

            val aggregated =
                dailyEntities.groupBy {
                    Triple(it.eventBrawlStarsId, it.tierRange, it.brawlerBrawlStarsIds)
                }

            val weeklyEntities =
                aggregated.map { (key, group) ->
                    val combinedRanks = group.map { it.rankCounts }.reduce { acc, r -> acc + r }
                    BrawlerTeamBattleRankStatsEntity(
                        eventBrawlStarsId = key.first,
                        periodType = StatsPeriodType.WEEKLY,
                        tierRange = key.second,
                        startBattleDate = startDate,
                        endBattleDate = endDate,
                        teamSize = key.third.size,
                        rankCounts = combinedRanks,
                        initialBrawlerBrawlStarsIds = key.third,
                    )
                }

            weeklyEntities.chunked(BATCH_SIZE).forEach { batch ->
                entityManager.transaction.begin()
                batch.forEach { entityManager.persist(it) }
                entityManager.transaction.commit()
                entityManager.clear()
            }

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
                                    .eq(StatsPeriodType.DAILY),
                                brawlerTeamBattleRankStatsEntity.startBattleDate
                                    .between(startDate, endDate),
                            ),
                    ),
                ).execute()
            queryFactory
                .delete(brawlerTeamBattleRankStatsEntity)
                .where(
                    brawlerTeamBattleRankStatsEntity.periodType.eq(StatsPeriodType.DAILY),
                    brawlerTeamBattleRankStatsEntity.startBattleDate
                        .between(startDate, endDate),
                ).execute()
            entityManager.transaction.commit()

            log.info(
                "[{}] Team Rank 주간 집계 완료 - 기간: {} ~ {}, 집계: {}건, 삭제: {}건",
                JOB_NAME,
                startDate,
                endDate,
                weeklyEntities.size,
                dailyEntities.size,
            )
        } finally {
            entityManager.close()
        }
    }

    private fun aggregateEnemyResult(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
    ) {
        val entityManager = entityManagerFactory.createEntityManager()

        try {
            val queryFactory = JPAQueryFactory(entityManager)

            val eventIds =
                queryFactory
                    .select(brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId)
                    .distinct()
                    .from(brawlerEnemyBattleResultStatsEntity)
                    .where(
                        brawlerEnemyBattleResultStatsEntity.periodType.eq(StatsPeriodType.DAILY),
                        brawlerEnemyBattleResultStatsEntity.startBattleDate
                            .between(startDate, endDate),
                    ).fetch()
                    .toList()

            var totalAggregated = 0L
            var totalDeleted = 0L

            for (eventId in eventIds) {
                val dailyEntities =
                    queryFactory
                        .selectFrom(brawlerEnemyBattleResultStatsEntity)
                        .where(
                            brawlerEnemyBattleResultStatsEntity.periodType
                                .eq(StatsPeriodType.DAILY),
                            brawlerEnemyBattleResultStatsEntity.startBattleDate
                                .between(startDate, endDate),
                            brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(eventId),
                        ).fetch()
                        .toList()

                val aggregated =
                    dailyEntities.groupBy {
                        Triple(it.tierRange, it.brawlerBrawlStarsId, it.enemyBrawlerBrawlStarsId)
                    }

                val weeklyEntities =
                    aggregated.map { (key, group) ->
                        val totalResult = group.map { it.resultCount }.reduce { acc, r -> acc + r }
                        BrawlerEnemyBattleResultStatsEntity(
                            eventBrawlStarsId = eventId,
                            periodType = StatsPeriodType.WEEKLY,
                            tierRange = key.first,
                            startBattleDate = startDate,
                            endBattleDate = endDate,
                            brawlerBrawlStarsId = key.second,
                            enemyBrawlerBrawlStarsId = key.third,
                            resultCount = totalResult,
                        )
                    }

                weeklyEntities.chunked(BATCH_SIZE).forEach { batch ->
                    entityManager.transaction.begin()
                    batch.forEach { entityManager.persist(it) }
                    entityManager.transaction.commit()
                    entityManager.clear()
                }
                totalAggregated += weeklyEntities.size

                entityManager.transaction.begin()
                val deleted =
                    queryFactory
                        .delete(brawlerEnemyBattleResultStatsEntity)
                        .where(
                            brawlerEnemyBattleResultStatsEntity.periodType
                                .eq(StatsPeriodType.DAILY),
                            brawlerEnemyBattleResultStatsEntity.startBattleDate
                                .between(startDate, endDate),
                            brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(eventId),
                        ).execute()
                entityManager.transaction.commit()
                totalDeleted += deleted
            }

            log.info(
                "[{}] Enemy Result 주간 집계 완료 - 기간: {} ~ {}, 집계: {}건, 삭제: {}건",
                JOB_NAME,
                startDate,
                endDate,
                totalAggregated,
                totalDeleted,
            )
        } finally {
            entityManager.close()
        }
    }
}
