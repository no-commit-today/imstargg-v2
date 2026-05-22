package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleRankStatsAggregator
import com.imstargg.infrastructure.db.core.statistics.QBrawlerBattleRankStatsEntity.Companion.brawlerBattleRankStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class BrawlerBattleRankStatsWriter(
    private val em: EntityManager,
) {
    @Transactional
    fun write(
        date: LocalDate,
        aggregator: BrawlerBattleRankStatsAggregator,
    ): Long {
        val queryFactory = JPAQueryFactory(em)
        queryFactory
            .delete(brawlerBattleRankStatsEntity)
            .where(brawlerBattleRankStatsEntity.battleDate.eq(date))
            .execute()

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
