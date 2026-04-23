package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.imstargg.core.enums.WeekRange
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus

class PerEventWeeklyBattleStatsTasklet<TBattle, TEntity : Any>(
    private val em: EntityManager,
    private val jobName: String,
    private val weekRangesSupplier: () -> List<WeekRange>,
    private val readDistinctEventIds: (JPAQueryFactory, WeekRange) -> List<Long>,
    private val readPage: (JPAQueryFactory, WeekRange, Long, Long) -> List<TBattle>,
    private val getId: (TBattle) -> Long,
    private val aggregatorFactory: (WeekRange) -> BattleStatsAggregator<TBattle, TEntity>,
    private val deleteExisting: (JPAQueryFactory, WeekRange, Long) -> Unit,
    private val persistAdditional: (EntityManager, TEntity) -> Unit = { _, _ -> },
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val weekRanges = weekRangesSupplier()
        log.info("[{}] 주간 범위 {}개 처리 시작", jobName, weekRanges.size)
        val queryFactory = JPAQueryFactory(em)

        for (weekRange in weekRanges) {
            processWeekRange(queryFactory, weekRange)
        }
        return RepeatStatus.FINISHED
    }

    private fun processWeekRange(
        queryFactory: JPAQueryFactory,
        weekRange: WeekRange,
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

            val entities = aggregator.toEntities()
            totalEntities += entities.size
            entities.chunked(BattleStatsConstants.BATCH_SIZE).forEach { batch ->
                batch.forEach {
                    em.persist(it)
                    persistAdditional(em, it)
                }
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
}
