package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerEnemyBattleResultStatsAggregator
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrawlerEnemyBattleResultStatsWriter(
    private val em: EntityManager,
) {
    @Transactional
    fun write(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        aggregator: BrawlerEnemyBattleResultStatsAggregator,
    ): Long {
        val queryFactory = JPAQueryFactory(em)
        queryFactory
            .delete(brawlerEnemyBattleResultStatsEntity)
            .where(
                brawlerEnemyBattleResultStatsEntity.startBattleDate.eq(weekRange.start),
                brawlerEnemyBattleResultStatsEntity.endBattleDate.eq(weekRange.end),
                brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
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
