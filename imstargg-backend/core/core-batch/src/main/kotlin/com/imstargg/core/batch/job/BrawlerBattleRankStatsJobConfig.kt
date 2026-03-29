package com.imstargg.core.batch.job

import com.imstargg.core.batch.domain.BrawlerBattleRankStatsAggregator
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.infrastructure.db.core.statistics.QBrawlerBattleRankStatsEntity.Companion.brawlerBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity.Companion.statsRankBattleEntity
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
class BrawlerBattleRankStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val JOB_NAME = "brawlerBattleRankStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerBattleRankStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerBattleRankStatsStep"
        private const val READ_PAGE_SIZE = 5000L
        private const val BATCH_SIZE = 1000
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerBattleRankStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
            .start(brawlerBattleRankStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerBattleRankStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                val entityManager = entityManagerFactory.createEntityManager()

                entityManager.use { entityManager ->
                    val queryFactory = JPAQueryFactory(entityManager)
                    val aggregator = BrawlerBattleRankStatsAggregator()

                    var lastId = 0L
                    var totalRead = 0L
                    while (true) {
                        entityManager.clear()
                        val battles =
                            queryFactory
                                .selectFrom(statsRankBattleEntity)
                                .where(
                                    statsRankBattleEntity.battleDate.eq(date),
                                    statsRankBattleEntity.id.gt(lastId),
                                ).orderBy(statsRankBattleEntity.id.asc())
                                .limit(READ_PAGE_SIZE)
                                .fetch()
                                .toList()

                        if (battles.isEmpty()) break
                        totalRead += battles.size
                        battles.forEach { aggregator.aggregate(it) }
                        lastId = battles.last().id!!
                        log.debug("[{}] 읽기 진행 - date: {}, 누적: {}건", JOB_NAME, date, totalRead)
                    }

                    entityManager.transaction.begin()
                    queryFactory
                        .delete(brawlerBattleRankStatsEntity)
                        .where(brawlerBattleRankStatsEntity.battleDate.eq(date))
                        .execute()
                    entityManager.transaction.commit()

                    log.debug("[{}] 집계 완료, 저장 시작 - date: {}", JOB_NAME, date)
                    val entities = aggregator.toEntities()
                    entities.chunked(BATCH_SIZE).forEach { batch ->
                        entityManager.transaction.begin()
                        batch.forEach { entityManager.persist(it) }
                        entityManager.transaction.commit()
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
