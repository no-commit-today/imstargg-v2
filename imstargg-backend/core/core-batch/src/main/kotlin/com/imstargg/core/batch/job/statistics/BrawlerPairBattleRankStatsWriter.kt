package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerPairBattleRankStatsAggregator
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleRankStatsEntity.Companion.brawlerPairBattleRankStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrawlerPairBattleRankStatsWriter(
    private val em: EntityManager,
) {
    @Transactional
    fun write(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        aggregator: BrawlerPairBattleRankStatsAggregator,
    ): Long {
        val queryFactory = JPAQueryFactory(em)
        queryFactory
            .delete(brawlerPairBattleRankStatsEntity)
            .where(
                brawlerPairBattleRankStatsEntity.startBattleDate.eq(weekRange.start),
                brawlerPairBattleRankStatsEntity.endBattleDate.eq(weekRange.end),
                brawlerPairBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
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
