package com.imstargg.core.batch.job.statistics.task

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DailyBattleStatsAggregationTask(
    private val entityManagerFactory: EntityManagerFactory,
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun <TBattle, TEntity : Any> aggregate(
        jobName: String,
        date: LocalDate,
        readPage: (JPAQueryFactory, LocalDate, Long) -> List<TBattle>,
        getId: (TBattle) -> Long,
        aggregator: BattleStatsAggregator<TBattle, TEntity>,
        deleteExisting: (JPAQueryFactory, LocalDate) -> Unit,
        persist: (EntityManager, TEntity) -> Unit = { entityManager, entity -> entityManager.persist(entity) },
    ) {
        var lastId = 0L
        var totalRead = 0L

        val readEm = entityManagerFactory.createEntityManager()
        try {
            val readQueryFactory = JPAQueryFactory(readEm)
            while (true) {
                val battles = readPage(readQueryFactory, date, lastId)
                if (battles.isEmpty()) break
                totalRead += battles.size
                battles.forEach { aggregator.aggregate(it) }
                lastId = getId(battles.last())
                readEm.clear()
                log.debug("[{}] 읽기 진행 - date: {}, 누적: {}건", jobName, date, totalRead)
            }
        } finally {
            readEm.close()
        }

        var totalEntities = 0L
        transactionRunner.run { em, queryFactory ->
            deleteExisting(queryFactory, date)

            aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
                batch.forEach { persist(em, it) }
                totalEntities += batch.size
                em.flush()
                em.clear()
            }
        }

        log.info(
            "[{}] 완료 - date: {}, 전투: {}건, 집계: {}건",
            jobName,
            date,
            totalRead,
            totalEntities,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyBattleStatsAggregationTask::class.java)
    }
}
