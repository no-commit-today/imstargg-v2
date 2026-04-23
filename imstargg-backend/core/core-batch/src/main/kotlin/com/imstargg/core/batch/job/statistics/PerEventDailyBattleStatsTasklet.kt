package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BattleStatsAggregator
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import java.time.LocalDate

/**
 * 이벤트별 일 단위 통계 집계 tasklet.
 *
 * **현재 사용처 없음**: 2026-04-23 기준 모든 통계 잡이 주간 집계
 * ([PerEventWeeklyBattleStatsTasklet]) 로 전환되었다. 본 클래스는 향후
 * 일 단위 이벤트별 집계가 다시 필요할 때를 위해 유지되며, 그때 이 패턴을
 * 그대로 재사용할 수 있도록 Spring-managed [EntityManager] 주입 방식을
 * 갖춘 상태로 보존한다.
 */
class PerEventDailyBattleStatsTasklet<TBattle, TEntity : Any>(
    private val em: EntityManager,
    private val jobName: String,
    private val dateSupplier: () -> LocalDate,
    private val readDistinctEventIds: (JPAQueryFactory, LocalDate) -> List<Long>,
    private val readPage: (JPAQueryFactory, LocalDate, Long, Long) -> List<TBattle>,
    private val getId: (TBattle) -> Long,
    private val aggregatorFactory: () -> BattleStatsAggregator<TBattle, TEntity>,
    private val deleteExisting: (JPAQueryFactory, LocalDate, Long) -> Unit,
    private val persistAdditional: (EntityManager, TEntity) -> Unit = { _, _ -> },
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val date = dateSupplier()
        val queryFactory = JPAQueryFactory(em)
        val eventIds = readDistinctEventIds(queryFactory, date)
        log.info("[{}] 이벤트 {}개 처리 시작 - date: {}", jobName, eventIds.size, date)

        var totalRead = 0L
        var totalEntities = 0L

        for (eventId in eventIds) {
            val aggregator = aggregatorFactory()

            var lastId = 0L
            while (true) {
                val battles = readPage(queryFactory, date, lastId, eventId)
                if (battles.isEmpty()) break
                totalRead += battles.size
                battles.forEach { aggregator.aggregate(it) }
                lastId = getId(battles.last())
                em.clear()
            }

            deleteExisting(queryFactory, date, eventId)

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
            "[{}] 완료 - date: {}, 전투: {}건, 집계: {}건",
            jobName,
            date,
            totalRead,
            totalEntities,
        )
        return RepeatStatus.FINISHED
    }
}
