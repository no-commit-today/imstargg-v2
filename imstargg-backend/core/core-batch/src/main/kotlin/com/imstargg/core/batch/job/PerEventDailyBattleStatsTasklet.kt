package com.imstargg.core.batch.job

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.LocalDate

class PerEventDailyBattleStatsTasklet<TBattle, TEntity>(
    private val entityManagerFactory: EntityManagerFactory,
    private val jobName: String,
    private val dateSupplier: () -> LocalDate,
    private val readDistinctEventIds: (JPAQueryFactory, LocalDate) -> List<Long>,
    private val readPage: (JPAQueryFactory, LocalDate, Long, Long) -> List<TBattle>,
    private val getId: (TBattle) -> Long,
    private val aggregatorFactory: () -> BattleStatsAggregator<TBattle, TEntity>,
    private val deleteExisting: (JPAQueryFactory, LocalDate, Long) -> Unit,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val READ_PAGE_SIZE = 5000L
        private const val BATCH_SIZE = 1000
    }

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val date = dateSupplier()
        val entityManager = entityManagerFactory.createEntityManager()

        entityManager.use { em ->
            val queryFactory = JPAQueryFactory(em)
            val eventIds = readDistinctEventIds(queryFactory, date)
            log.info("[{}] 이벤트 {}개 처리 시작 - date: {}", jobName, eventIds.size, date)

            var totalRead = 0L
            var totalEntities = 0L

            for (eventId in eventIds) {
                val aggregator = aggregatorFactory()

                var lastId = 0L
                while (true) {
                    em.clear()
                    val battles = readPage(queryFactory, date, lastId, eventId)

                    if (battles.isEmpty()) break
                    totalRead += battles.size
                    battles.forEach { aggregator.aggregate(it) }
                    lastId = getId(battles.last())
                    log.debug(
                        "[{}] 읽기 진행 - date: {}, event: {}, 누적: {}건",
                        jobName,
                        date,
                        eventId,
                        totalRead,
                    )
                }

                em.transaction.begin()
                deleteExisting(queryFactory, date, eventId)
                em.transaction.commit()

                val entities = aggregator.toEntities()
                totalEntities += entities.size
                entities.chunked(BATCH_SIZE).forEach { batch ->
                    em.transaction.begin()
                    batch.forEach { em.persist(it) }
                    em.transaction.commit()
                    em.clear()
                }

                log.debug(
                    "[{}] 이벤트 완료 - date: {}, event: {}, 집계: {}건",
                    jobName,
                    date,
                    eventId,
                    entities.size,
                )
            }

            log.info(
                "[{}] 완료 - date: {}, 전투: {}건, 집계: {}건",
                jobName,
                date,
                totalRead,
                totalEntities,
            )
        }
        return RepeatStatus.FINISHED
    }
}
