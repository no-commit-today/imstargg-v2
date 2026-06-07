package com.imstargg.core.batch.job.statistics.enemybattleresult

import com.imstargg.core.batch.domain.statistics.BrawlerEnemyBattleResultStatsAggregator
import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import org.springframework.stereotype.Component

@Component
class BrawlerEnemyBattleResultStatsWriter(
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun write(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        aggregator: BrawlerEnemyBattleResultStatsAggregator,
    ): Long {
        var totalEntities = 0L
        transactionRunner.run { em, queryFactory ->
            queryFactory
                .delete(brawlerEnemyBattleResultStatsEntity)
                .where(
                    brawlerEnemyBattleResultStatsEntity.startBattleDate.eq(weekRange.start),
                    brawlerEnemyBattleResultStatsEntity.endBattleDate.eq(weekRange.end),
                    brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
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
