package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerPairBattleResultStatsAggregator
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleResultStatsEntity.Companion.brawlerPairBattleResultStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrawlerPairBattleResultStatsWriter(
    private val em: EntityManager,
) {
    @Transactional
    fun write(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        aggregator: BrawlerPairBattleResultStatsAggregator,
    ): Long {
        val queryFactory = JPAQueryFactory(em)
        queryFactory
            .delete(brawlerPairBattleResultStatsEntity)
            .where(
                brawlerPairBattleResultStatsEntity.startBattleDate.eq(weekRange.start),
                brawlerPairBattleResultStatsEntity.endBattleDate.eq(weekRange.end),
                brawlerPairBattleResultStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
            ).execute()

        var totalEntities = 0L
        aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
            batch.forEach { em.persist(it) }
            totalEntities += batch.size
            em.flush()
            em.clear()
        }
        return totalEntities
    }
}
