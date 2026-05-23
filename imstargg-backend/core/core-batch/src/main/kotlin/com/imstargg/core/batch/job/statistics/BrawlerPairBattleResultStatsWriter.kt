package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerPairBattleResultStatsAggregator
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleResultStatsEntity.Companion.brawlerPairBattleResultStatsEntity
import org.springframework.stereotype.Component

@Component
class BrawlerPairBattleResultStatsWriter(
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun write(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        aggregator: BrawlerPairBattleResultStatsAggregator,
    ): Long {
        var totalEntities = 0L
        transactionRunner.run { em, queryFactory ->
            queryFactory
                .delete(brawlerPairBattleResultStatsEntity)
                .where(
                    brawlerPairBattleResultStatsEntity.startBattleDate.eq(weekRange.start),
                    brawlerPairBattleResultStatsEntity.endBattleDate.eq(weekRange.end),
                    brawlerPairBattleResultStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                ).execute()

            aggregator.flushEntityChunks(BattleStatsConstants.BATCH_SIZE) { batch ->
                batch.forEach { em.persist(it) }
                totalEntities += batch.size
                em.flush()
                em.clear()
            }
        }
        return totalEntities
    }
}
