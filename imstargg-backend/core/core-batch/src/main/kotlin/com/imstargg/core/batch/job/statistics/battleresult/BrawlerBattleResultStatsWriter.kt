package com.imstargg.core.batch.job.statistics.battleresult

import com.imstargg.core.batch.domain.statistics.BrawlerBattleResultStatsAggregator
import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import com.imstargg.infrastructure.db.core.statistics.QBrawlerBattleResultStatsEntity.Companion.brawlerBattleResultStatsEntity
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BrawlerBattleResultStatsWriter(
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun write(
        date: LocalDate,
        aggregator: BrawlerBattleResultStatsAggregator,
    ): Long {
        var totalEntities = 0L
        transactionRunner.run { em, queryFactory ->
            queryFactory
                .delete(brawlerBattleResultStatsEntity)
                .where(brawlerBattleResultStatsEntity.battleDate.eq(date))
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
