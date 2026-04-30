package com.imstargg.core.batch.job.statistics.task

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.enums.WeekRange
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class WeeklyEventBattleStatsAggregationTask {
    fun <TBattle, TEntity : Any> aggregate(
        em: EntityManager,
        jobName: String,
        weekRanges: List<WeekRange>,
        readDistinctEventIds: (JPAQueryFactory, WeekRange) -> List<Long>,
        readPage: (JPAQueryFactory, WeekRange, Long, Long) -> List<TBattle>,
        getId: (TBattle) -> Long,
        aggregatorFactory: (WeekRange) -> BattleStatsAggregator<TBattle, TEntity>,
        deleteExisting: (JPAQueryFactory, WeekRange, Long) -> Unit,
        persist: (EntityManager, TEntity) -> Unit = { entityManager, entity -> entityManager.persist(entity) },
        persistAdditional: (EntityManager, TEntity) -> Unit = { _, _ -> },
    ) {
        log.info("[{}] 주간 범위 {}개 처리 시작", jobName, weekRanges.size)
        val queryFactory = JPAQueryFactory(em)

        for (weekRange in weekRanges) {
            processWeekRange(
                em = em,
                jobName = jobName,
                queryFactory = queryFactory,
                weekRange = weekRange,
                readDistinctEventIds = readDistinctEventIds,
                readPage = readPage,
                getId = getId,
                aggregatorFactory = aggregatorFactory,
                deleteExisting = deleteExisting,
                persist = persist,
                persistAdditional = persistAdditional,
            )
        }
    }

    private fun <TBattle, TEntity : Any> processWeekRange(
        em: EntityManager,
        jobName: String,
        queryFactory: JPAQueryFactory,
        weekRange: WeekRange,
        readDistinctEventIds: (JPAQueryFactory, WeekRange) -> List<Long>,
        readPage: (JPAQueryFactory, WeekRange, Long, Long) -> List<TBattle>,
        getId: (TBattle) -> Long,
        aggregatorFactory: (WeekRange) -> BattleStatsAggregator<TBattle, TEntity>,
        deleteExisting: (JPAQueryFactory, WeekRange, Long) -> Unit,
        persist: (EntityManager, TEntity) -> Unit,
        persistAdditional: (EntityManager, TEntity) -> Unit,
    ) {
        val eventIds = readDistinctEventIds(queryFactory, weekRange)
        log.info(
            "[{}] 이벤트 {}개 처리 시작 - weekRange: {} ~ {}",
            jobName,
            eventIds.size,
            weekRange.start,
            weekRange.end,
        )

        var totalRead = 0L
        var totalEntities = 0L

        for (eventId in eventIds) {
            val aggregator = aggregatorFactory(weekRange)

            var lastId = 0L
            while (true) {
                val battles = readPage(queryFactory, weekRange, lastId, eventId)
                if (battles.isEmpty()) break
                totalRead += battles.size
                battles.forEach { aggregator.aggregate(it) }
                lastId = getId(battles.last())
                em.clear()
            }

            deleteExisting(queryFactory, weekRange, eventId)

            aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
                batch.forEach {
                    persist(em, it)
                    persistAdditional(em, it)
                }
                totalEntities += batch.size
                em.flush()
                em.clear()
            }
        }

        log.info(
            "[{}] 주간 범위 완료 - weekRange: {} ~ {}, 전투: {}건, 집계: {}건",
            jobName,
            weekRange.start,
            weekRange.end,
            totalRead,
            totalEntities,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(WeeklyEventBattleStatsAggregationTask::class.java)
    }
}
