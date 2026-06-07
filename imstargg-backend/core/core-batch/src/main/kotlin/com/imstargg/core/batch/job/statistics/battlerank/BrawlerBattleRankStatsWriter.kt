package com.imstargg.core.batch.job.statistics.battlerank

import com.imstargg.core.batch.domain.statistics.BrawlerBattleRankStatsAggregator
import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import com.imstargg.infrastructure.db.core.statistics.QBrawlerBattleRankStatsEntity.Companion.brawlerBattleRankStatsEntity
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BrawlerBattleRankStatsWriter(
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun write(
        date: LocalDate,
        aggregator: BrawlerBattleRankStatsAggregator,
    ): Long {
        var totalEntities = 0L
        transactionRunner.run { em, queryFactory ->
            queryFactory
                .delete(brawlerBattleRankStatsEntity)
                .where(brawlerBattleRankStatsEntity.battleDate.eq(date))
                .execute()

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
