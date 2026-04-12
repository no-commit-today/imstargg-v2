package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.LocalDate

class DailyBattleStatsTasklet<TBattle, TEntity>(
    private val entityManagerFactory: EntityManagerFactory,
    private val jobName: String,
    private val dateSupplier: () -> LocalDate,
    private val readPage: (JPAQueryFactory, LocalDate, Long) -> List<TBattle>,
    private val getId: (TBattle) -> Long,
    private val aggregatorFactory: () -> BattleStatsAggregator<TBattle, TEntity>,
    private val deleteExisting: (JPAQueryFactory, LocalDate) -> Unit,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val date = dateSupplier()
        val entityManager = entityManagerFactory.createEntityManager()

        entityManager.use { em ->
            val queryFactory = JPAQueryFactory(em)
            val aggregator = aggregatorFactory()

            var lastId = 0L
            var totalRead = 0L
            while (true) {
                em.clear()
                val battles = readPage(queryFactory, date, lastId)

                if (battles.isEmpty()) break
                totalRead += battles.size
                battles.forEach { aggregator.aggregate(it) }
                lastId = getId(battles.last())
                log.debug("[{}] 읽기 진행 - date: {}, 누적: {}건", jobName, date, totalRead)
            }

            em.transaction.begin()
            deleteExisting(queryFactory, date)
            em.transaction.commit()

            log.debug("[{}] 집계 완료, 저장 시작 - date: {}", jobName, date)
            val entities = aggregator.toEntities()
            entities.chunked(BattleStatsConstants.BATCH_SIZE).forEach { batch ->
                em.transaction.begin()
                batch.forEach { em.persist(it) }
                em.transaction.commit()
                em.clear()
            }

            log.info(
                "[{}] 완료 - date: {}, 전투: {}건, 집계: {}건",
                jobName,
                date,
                totalRead,
                entities.size,
            )
        }
        return RepeatStatus.FINISHED
    }
}
