package com.imstargg.core.batch.job

import com.imstargg.core.batch.domain.BrawlerEnemyBattleResultStatsAggregator
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity.Companion.statsResultBattleEntity
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
class BrawlerEnemyBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val JOB_NAME = "brawlerEnemyBattleResultStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerEnemyBattleResultStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerEnemyBattleResultStatsStep"
        private const val READ_PAGE_SIZE = 5000L
        private const val BATCH_SIZE = 1000
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerEnemyBattleResultStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
            .start(brawlerEnemyBattleResultStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerEnemyBattleResultStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                val entityManager = entityManagerFactory.createEntityManager()

                entityManager.use { em ->
                    val queryFactory = JPAQueryFactory(em)
                    val aggregator = BrawlerEnemyBattleResultStatsAggregator()

                    var lastId = 0L
                    var totalRead = 0L
                    while (true) {
                        em.clear()
                        val battles =
                            queryFactory
                                .selectFrom(statsResultBattleEntity)
                                .where(
                                    statsResultBattleEntity.battleDate.eq(date),
                                    statsResultBattleEntity.id.gt(lastId),
                                ).orderBy(statsResultBattleEntity.id.asc())
                                .limit(READ_PAGE_SIZE)
                                .fetch()
                                .toList()

                        if (battles.isEmpty()) break
                        totalRead += battles.size
                        battles.forEach { aggregator.aggregate(it) }
                        lastId = battles.last().id!!
                        log.debug("[{}] 읽기 진행 - date: {}, 누적: {}건", JOB_NAME, date, totalRead)
                    }

                    em.transaction.begin()
                    queryFactory
                        .delete(brawlerEnemyBattleResultStatsEntity)
                        .where(
                            brawlerEnemyBattleResultStatsEntity.periodType
                                .eq(StatsPeriodType.DAILY),
                            brawlerEnemyBattleResultStatsEntity.startBattleDate.eq(date),
                        ).execute()
                    em.transaction.commit()

                    log.debug("[{}] 집계 완료, 저장 시작 - date: {}", JOB_NAME, date)
                    val entities = aggregator.toEntities()
                    entities.chunked(BATCH_SIZE).forEach { batch ->
                        em.transaction.begin()
                        batch.forEach { em.persist(it) }
                        em.transaction.commit()
                        em.clear()
                    }

                    log.info(
                        "[{}] 완료 - date: {}, 전투: {}건, 집계: {}건",
                        JOB_NAME,
                        date,
                        totalRead,
                        entities.size,
                    )
                }
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
