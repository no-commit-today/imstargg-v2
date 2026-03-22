package com.imstargg.core.batch.job

import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.infrastructure.db.core.statistics.BrawlerDuoBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTrioBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerDuoBattleRankStatsEntity.Companion.brawlerDuoBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsEntity.Companion.brawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTrioBattleRankStatsEntity.Companion.brawlerTrioBattleRankStatsEntity
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
        private const val STEP_DUO_RANK = "monthlyStatsAggregationDuoRankStep"
        private const val STEP_TRIO_RANK = "monthlyStatsAggregationTrioRankStep"
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
            .next(monthlyStatsAggregationDuoRankStep())
            .next(monthlyStatsAggregationTrioRankStep())
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
                                    entity.teammateBrawlStarsId1,
                                    entity.teammateBrawlStarsId2,
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
                                    entity.teammateBrawlStarsId1,
                                    entity.teammateBrawlStarsId2,
                                ).fetch()

                        val monthlyEntities =
                            aggregated.map { tuple ->
                                BrawlerTeamBattleResultStatsEntity.create(
                                    eventBrawlStarsId = tuple.get(entity.eventBrawlStarsId)!!,
                                    periodType = StatsPeriodType.MONTHLY,
                                    tierRange = tuple.get(entity.tierRange)!!,
                                    startAt = monthlyStartAt,
                                    brawlerBrawlStarsId = tuple.get(entity.brawlerBrawlStarsId)!!,
                                    teammateBrawlStarsIds =
                                        listOf(
                                            tuple.get(entity.teammateBrawlStarsId1)!!,
                                            tuple.get(entity.teammateBrawlStarsId2)!!,
                                        ),
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

    @Bean(STEP_DUO_RANK)
    @JobScope
    fun monthlyStatsAggregationDuoRankStep(): Step =
        StepBuilder(STEP_DUO_RANK, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                val yearMonth = YearMonth.from(date)
                val startDate = yearMonth.atDay(1)
                val endDate = yearMonth.atEndOfMonth()
                val monthlyStartAt = startDate.atStartOfDay().atOffset(ZoneOffset.UTC)
                val entity = brawlerDuoBattleRankStatsEntity
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
                        val rank1Sum = entity.rankCount1.longSum()
                        val rank2Sum = entity.rankCount2.longSum()
                        val rank3Sum = entity.rankCount3.longSum()
                        val rank4Sum = entity.rankCount4.longSum()
                        val rank5Sum = entity.rankCount5.longSum()

                        val aggregated =
                            queryFactory
                                .select(
                                    entity.eventBrawlStarsId,
                                    entity.tierRange,
                                    entity.brawlerBrawlStarsId,
                                    entity.teammateBrawlStarsId,
                                    rank1Sum,
                                    rank2Sum,
                                    rank3Sum,
                                    rank4Sum,
                                    rank5Sum,
                                ).from(entity)
                                .where(
                                    entity.periodType.eq(StatsPeriodType.WEEKLY),
                                    entity.date.between(startDate, endDate),
                                    entity.eventBrawlStarsId.eq(eventId),
                                ).groupBy(
                                    entity.eventBrawlStarsId,
                                    entity.tierRange,
                                    entity.brawlerBrawlStarsId,
                                    entity.teammateBrawlStarsId,
                                ).fetch()

                        val monthlyEntities =
                            aggregated.map { tuple ->
                                BrawlerDuoBattleRankStatsEntity.create(
                                    eventBrawlStarsId = tuple.get(entity.eventBrawlStarsId)!!,
                                    periodType = StatsPeriodType.MONTHLY,
                                    tierRange = tuple.get(entity.tierRange)!!,
                                    startAt = monthlyStartAt,
                                    brawlerBrawlStarsId = tuple.get(entity.brawlerBrawlStarsId)!!,
                                    teammateBrawlStarsId = tuple.get(entity.teammateBrawlStarsId)!!,
                                    rankCount1 = tuple.get(rank1Sum)!!,
                                    rankCount2 = tuple.get(rank2Sum)!!,
                                    rankCount3 = tuple.get(rank3Sum)!!,
                                    rankCount4 = tuple.get(rank4Sum)!!,
                                    rankCount5 = tuple.get(rank5Sum)!!,
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
                        "[{}] Duo Rank 월간 집계 완료 - 대상월: {}, 집계: {}건, 삭제: {}건",
                        JOB_NAME,
                        yearMonth,
                        totalAggregated,
                        totalDeleted,
                    )
                    alertManager.alert(
                        AlertCommand
                            .builder()
                            .warn()
                            .title("[$JOB_NAME] Duo Rank 월간 집계 완료")
                            .content("대상월: $yearMonth, 집계: ${totalAggregated}건, 삭제: ${totalDeleted}건")
                            .build(),
                    )
                } finally {
                    entityManager.close()
                }
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(STEP_TRIO_RANK)
    @JobScope
    fun monthlyStatsAggregationTrioRankStep(): Step =
        StepBuilder(STEP_TRIO_RANK, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                val yearMonth = YearMonth.from(date)
                val startDate = yearMonth.atDay(1)
                val endDate = yearMonth.atEndOfMonth()
                val monthlyStartAt = startDate.atStartOfDay().atOffset(ZoneOffset.UTC)
                val entity = brawlerTrioBattleRankStatsEntity
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
                        val rank1Sum = entity.rankCount1.longSum()
                        val rank2Sum = entity.rankCount2.longSum()
                        val rank3Sum = entity.rankCount3.longSum()
                        val rank4Sum = entity.rankCount4.longSum()

                        val aggregated =
                            queryFactory
                                .select(
                                    entity.eventBrawlStarsId,
                                    entity.tierRange,
                                    entity.brawlerBrawlStarsId,
                                    entity.teammateBrawlStarsId1,
                                    entity.teammateBrawlStarsId2,
                                    rank1Sum,
                                    rank2Sum,
                                    rank3Sum,
                                    rank4Sum,
                                ).from(entity)
                                .where(
                                    entity.periodType.eq(StatsPeriodType.WEEKLY),
                                    entity.date.between(startDate, endDate),
                                    entity.eventBrawlStarsId.eq(eventId),
                                ).groupBy(
                                    entity.eventBrawlStarsId,
                                    entity.tierRange,
                                    entity.brawlerBrawlStarsId,
                                    entity.teammateBrawlStarsId1,
                                    entity.teammateBrawlStarsId2,
                                ).fetch()

                        val monthlyEntities =
                            aggregated.map { tuple ->
                                BrawlerTrioBattleRankStatsEntity.create(
                                    eventBrawlStarsId = tuple.get(entity.eventBrawlStarsId)!!,
                                    periodType = StatsPeriodType.MONTHLY,
                                    tierRange = tuple.get(entity.tierRange)!!,
                                    startAt = monthlyStartAt,
                                    brawlerBrawlStarsId = tuple.get(entity.brawlerBrawlStarsId)!!,
                                    teammateBrawlStarsIds =
                                        listOf(
                                            tuple.get(entity.teammateBrawlStarsId1)!!,
                                            tuple.get(entity.teammateBrawlStarsId2)!!,
                                        ),
                                    rankCount1 = tuple.get(rank1Sum)!!,
                                    rankCount2 = tuple.get(rank2Sum)!!,
                                    rankCount3 = tuple.get(rank3Sum)!!,
                                    rankCount4 = tuple.get(rank4Sum)!!,
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
                        "[{}] Trio Rank 월간 집계 완료 - 대상월: {}, 집계: {}건, 삭제: {}건",
                        JOB_NAME,
                        yearMonth,
                        totalAggregated,
                        totalDeleted,
                    )
                    alertManager.alert(
                        AlertCommand
                            .builder()
                            .warn()
                            .title("[$JOB_NAME] Trio Rank 월간 집계 완료")
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
