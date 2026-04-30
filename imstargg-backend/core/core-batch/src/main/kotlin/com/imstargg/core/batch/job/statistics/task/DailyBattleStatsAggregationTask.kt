package com.imstargg.core.batch.job.statistics.task

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DailyBattleStatsAggregationTask {
    fun <TBattle, TEntity : Any> aggregate(
        em: EntityManager,
        jobName: String,
        date: LocalDate,
        readPage: (JPAQueryFactory, LocalDate, Long) -> List<TBattle>,
        getId: (TBattle) -> Long,
        aggregator: BattleStatsAggregator<TBattle, TEntity>,
        deleteExisting: (JPAQueryFactory, LocalDate) -> Unit,
        persist: (EntityManager, TEntity) -> Unit = { entityManager, entity -> entityManager.persist(entity) },
    ) {
        val queryFactory = JPAQueryFactory(em)

        var lastId = 0L
        var totalRead = 0L
        while (true) {
            val battles = readPage(queryFactory, date, lastId)
            if (battles.isEmpty()) break
            totalRead += battles.size
            battles.forEach { aggregator.aggregate(it) }
            lastId = getId(battles.last())
            em.clear()
            log.debug("[{}] 읽기 진행 - date: {}, 누적: {}건", jobName, date, totalRead)
        }

        deleteExisting(queryFactory, date)

        var totalEntities = 0L
        aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
            batch.forEach { persist(em, it) }
            totalEntities += batch.size
            em.flush()
            em.clear()
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
