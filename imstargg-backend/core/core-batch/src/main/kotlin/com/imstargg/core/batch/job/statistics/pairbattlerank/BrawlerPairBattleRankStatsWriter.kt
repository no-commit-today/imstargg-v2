package com.imstargg.core.batch.job.statistics.pairbattlerank

import com.imstargg.core.batch.domain.statistics.BrawlerPairBattleRankStatsAggregator
import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleRankStatsEntity.Companion.brawlerPairBattleRankStatsEntity
import org.springframework.stereotype.Component

@Component
class BrawlerPairBattleRankStatsWriter(
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun write(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        aggregator: BrawlerPairBattleRankStatsAggregator,
    ): Long {
        var totalEntities = 0L
        transactionRunner.run { em, queryFactory ->
            queryFactory
                .delete(brawlerPairBattleRankStatsEntity)
                .where(
                    brawlerPairBattleRankStatsEntity.startBattleDate.eq(weekRange.start),
                    brawlerPairBattleRankStatsEntity.endBattleDate.eq(weekRange.end),
                    brawlerPairBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
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
