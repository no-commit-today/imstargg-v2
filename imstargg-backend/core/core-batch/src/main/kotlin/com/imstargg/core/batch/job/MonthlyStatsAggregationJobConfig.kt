package com.imstargg.core.batch.job

import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsEntity.Companion.brawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsEntity.Companion.brawlerTeamBattleResultStatsEntity
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.core.types.dsl.NumberPath
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
import java.time.YearMonth
import java.time.ZoneOffset

@Configuration
class MonthlyStatsAggregationJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
    private val alertManager: AlertManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val JOB_NAME = "monthlyStatsAggregationJob"
        const val DATE_PARAMETER_NAME = "monthlyStatsAggregationJobDateJobParameter"
        private const val STEP_TEAM_RESULT = "monthlyStatsAggregationTeamResultStep"
        private const val STEP_ENEMY_RESULT = "monthlyStatsAggregationEnemyResultStep"
        private const val STEP_TEAM_RANK = "monthlyStatsAggregationTeamRankStep"
        private const val BATCH_SIZE = 1000
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun monthlyStatsAggregationJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
            .start(monthlyStatsAggregationTeamResultStep())
            .next(monthlyStatsAggregationEnemyResultStep())
            .next(monthlyStatsAggregationTeamRankStep())
            .build()

    @Bean(STEP_TEAM_RESULT)
    @JobScope
    fun monthlyStatsAggregationTeamResultStep(): Step =
        StepBuilder(STEP_TEAM_RESULT, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                val yearMonth = YearMonth.from(date)
                val startDate = yearMonth.atDay(1)
                val endDate = yearMonth.atEndOfMonth()
                val monthlyStartAt = startDate.atStartOfDay().atOffset(ZoneOffset.UTC)
                val entity = brawlerTeamBattleResultStatsEntity
                val entityManager = entityManagerFactory.createEntityManager()

                try {
                    val queryFactory = JPAQueryFactory(entityManager)

                    val eventIds =
                        queryFactory
                            .select(entity.eventBrawlStarsId)
                            .distinct()
                            .from(entity)
                            .where(
                                entity.periodType.eq(StatsPeriodType.WEEKLY),
                                entity.date.between(startDate, endDate),
                            ).fetch()

                    var totalAggregated = 0L
                    var totalDeleted = 0L

                    for (eventId in eventIds) {
                        val victorySum = entity.victoryCount.longSum()
                        val defeatSum = entity.defeatCount.longSum()
                        val drawSum = entity.drawCount.longSum()

                        val aggregated =
                            queryFactory
                                .select(
                                    entity.eventBrawlStarsId,
                                    entity.tierRange,
                                    entity.brawlerBrawlStarsId,
                                    entity.teammateBrawlStarsIds,
                                    victorySum,
                                    defeatSum,
                                    drawSum,
                                ).from(entity)
                                .where(
                                    entity.periodType.eq(StatsPeriodType.WEEKLY),
                                    entity.date.between(startDate, endDate),
                                    entity.eventBrawlStarsId.eq(eventId),
                                ).groupBy(
                                    entity.eventBrawlStarsId,
                                    entity.tierRange,
                                    entity.brawlerBrawlStarsId,
                                    entity.teammateBrawlStarsIds,
                                ).fetch()

                        val monthlyEntities =
                            aggregated.map { tuple ->
                                BrawlerTeamBattleResultStatsEntity.create(
                                    eventBrawlStarsId = tuple.get(entity.eventBrawlStarsId)!!,
                                    periodType = StatsPeriodType.MONTHLY,
                                    tierRange = tuple.get(entity.tierRange)!!,
                                    startAt = monthlyStartAt,
                                    brawlerBrawlStarsId = tuple.get(entity.brawlerBrawlStarsId)!!,
                                    teammateBrawlStarsIds = tuple.get(entity.teammateBrawlStarsIds)!!,
                                    victoryCount = tuple.get(victorySum)!!,
                                    defeatCount = tuple.get(defeatSum)!!,
                                    drawCount = tuple.get(drawSum)!!,
                                )
                            }

                        monthlyEntities.chunked(BATCH_SIZE).forEach { batch ->
                            entityManager.transaction.begin()
                            batch.forEach { entityManager.persist(it) }
                            entityManager.transaction.commit()
                        }
                        totalAggregated += monthlyEntities.size

                        entityManager.transaction.begin()
                        val deleted =
                            queryFactory
                                .delete(entity)
                                .where(
                                    entity.periodType.eq(StatsPeriodType.WEEKLY),
                                    entity.date.between(startDate, endDate),
                                    entity.eventBrawlStarsId.eq(eventId),
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
                    alertManager.alert(
                        AlertCommand
                            .builder()
                            .warn()
                            .title("[$JOB_NAME] Team Result 월간 집계 완료")
                            .content("대상월: $yearMonth, 집계: ${totalAggregated}건, 삭제: ${totalDeleted}건")
                            .build(),
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
                val date = dateJobParameter().date!!
                val yearMonth = YearMonth.from(date)
                val startDate = yearMonth.atDay(1)
                val endDate = yearMonth.atEndOfMonth()
                val monthlyStartAt = startDate.atStartOfDay().atOffset(ZoneOffset.UTC)
                val entity = brawlerEnemyBattleResultStatsEntity
                val entityManager = entityManagerFactory.createEntityManager()

                try {
                    val queryFactory = JPAQueryFactory(entityManager)

                    val eventIds =
                        queryFactory
                            .select(entity.eventBrawlStarsId)
                            .distinct()
                            .from(entity)
                            .where(
                                entity.periodType.eq(StatsPeriodType.WEEKLY),
                                entity.date.between(startDate, endDate),
                            ).fetch()

                    var totalAggregated = 0L
                    var totalDeleted = 0L

                    for (eventId in eventIds) {
                        val victorySum = entity.victoryCount.longSum()
                        val defeatSum = entity.defeatCount.longSum()
                        val drawSum = entity.drawCount.longSum()

                        val aggregated =
                            queryFactory
                                .select(
                                    entity.eventBrawlStarsId,
                                    entity.tierRange,
                                    entity.brawlerBrawlStarsId,
                                    entity.enemyBrawlerBrawlStarsId,
                                    victorySum,
                                    defeatSum,
                                    drawSum,
                                ).from(entity)
                                .where(
                                    entity.periodType.eq(StatsPeriodType.WEEKLY),
                                    entity.date.between(startDate, endDate),
                                    entity.eventBrawlStarsId.eq(eventId),
                                ).groupBy(
                                    entity.eventBrawlStarsId,
                                    entity.tierRange,
                                    entity.brawlerBrawlStarsId,
                                    entity.enemyBrawlerBrawlStarsId,
                                ).fetch()

                        val monthlyEntities =
                            aggregated.map { tuple ->
                                BrawlerEnemyBattleResultStatsEntity.create(
                                    eventBrawlStarsId = tuple.get(entity.eventBrawlStarsId)!!,
                                    periodType = StatsPeriodType.MONTHLY,
                                    tierRange = tuple.get(entity.tierRange)!!,
                                    startAt = monthlyStartAt,
                                    brawlerBrawlStarsId = tuple.get(entity.brawlerBrawlStarsId)!!,
                                    enemyBrawlerBrawlStarsId = tuple.get(entity.enemyBrawlerBrawlStarsId)!!,
                                    victoryCount = tuple.get(victorySum)!!,
                                    defeatCount = tuple.get(defeatSum)!!,
                                    drawCount = tuple.get(drawSum)!!,
                                )
                            }

                        monthlyEntities.chunked(BATCH_SIZE).forEach { batch ->
                            entityManager.transaction.begin()
                            batch.forEach { entityManager.persist(it) }
                            entityManager.transaction.commit()
                        }
                        totalAggregated += monthlyEntities.size

                        entityManager.transaction.begin()
                        val deleted =
                            queryFactory
                                .delete(entity)
                                .where(
                                    entity.periodType.eq(StatsPeriodType.WEEKLY),
                                    entity.date.between(startDate, endDate),
                                    entity.eventBrawlStarsId.eq(eventId),
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
                    alertManager.alert(
                        AlertCommand
                            .builder()
                            .warn()
                            .title("[$JOB_NAME] Enemy Result 월간 집계 완료")
                            .content("대상월: $yearMonth, 집계: ${totalAggregated}건, 삭제: ${totalDeleted}건")
                            .build(),
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
                val date = dateJobParameter().date!!
                val yearMonth = YearMonth.from(date)
                val startDate = yearMonth.atDay(1)
                val endDate = yearMonth.atEndOfMonth()
                val monthlyStartAt = startDate.atStartOfDay().atOffset(ZoneOffset.UTC)
                val entity = brawlerTeamBattleRankStatsEntity
                val entityManager = entityManagerFactory.createEntityManager()

                try {
                    val queryFactory = JPAQueryFactory(entityManager)

                    val eventIds =
                        queryFactory
                            .select(entity.eventBrawlStarsId)
                            .distinct()
                            .from(entity)
                            .where(
                                entity.periodType.eq(StatsPeriodType.WEEKLY),
                                entity.date.between(startDate, endDate),
                            ).fetch()

                    var totalAggregated = 0L
                    var totalDeleted = 0L

                    for (eventId in eventIds) {
                        val weeklyEntities =
                            queryFactory
                                .selectFrom(entity)
                                .where(
                                    entity.periodType.eq(StatsPeriodType.WEEKLY),
                                    entity.date.between(startDate, endDate),
                                    entity.eventBrawlStarsId.eq(eventId),
                                ).fetch()

                        val aggregated =
                            weeklyEntities.groupBy {
                                Triple(it.tierRange, it.brawlerBrawlStarsId, it.teammateBrawlStarsIds)
                            }

                        val monthlyEntities =
                            aggregated.map { (key, group) ->
                                val combinedRanks = group.map { it.rankCounts }.reduce { acc, r -> acc + r }
                                BrawlerTeamBattleRankStatsEntity.create(
                                    eventBrawlStarsId = eventId,
                                    periodType = StatsPeriodType.MONTHLY,
                                    tierRange = key.first,
                                    startAt = monthlyStartAt,
                                    brawlerBrawlStarsId = key.second,
                                    teammateBrawlStarsIds = key.third,
                                    rankCounts = combinedRanks,
                                )
                            }

                        monthlyEntities.chunked(BATCH_SIZE).forEach { batch ->
                            entityManager.transaction.begin()
                            batch.forEach { entityManager.persist(it) }
                            entityManager.transaction.commit()
                        }
                        totalAggregated += monthlyEntities.size

                        entityManager.transaction.begin()
                        val deleted =
                            queryFactory
                                .delete(entity)
                                .where(
                                    entity.periodType.eq(StatsPeriodType.WEEKLY),
                                    entity.date.between(startDate, endDate),
                                    entity.eventBrawlStarsId.eq(eventId),
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
                    alertManager.alert(
                        AlertCommand
                            .builder()
                            .warn()
                            .title("[$JOB_NAME] Team Rank 월간 집계 완료")
                            .content("대상월: $yearMonth, 집계: ${totalAggregated}건, 삭제: ${totalDeleted}건")
                            .build(),
                    )
                } finally {
                    entityManager.close()
                }
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    private fun NumberPath<Long>.longSum(): NumberExpression<Long> =
        Expressions.numberOperation(Long::class.javaObjectType, com.querydsl.core.types.Ops.AggOps.SUM_AGG, this)
}
