package com.imstargg.core.batch.job.statistics.pairbattleresult

import com.imstargg.core.batch.domain.statistics.BrawlerPairBattleResultStatsAggregator
import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleResultStatsBackupEntity.Companion.brawlerPairBattleResultStatsBackupEntity
import org.springframework.stereotype.Component

/**
 * 주 버킷 하나를 한 트랜잭션(REQUIRES_NEW)에서 통째로 교체한다 — 버킷의 기존 행을 지우고 집계 결과를 chunk 단위로 넣는다.
 * 다른 버킷의 행은 건드리지 않는다.
 */
@Component
class BrawlerPairBattleResultStatsWriter(
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun write(
        weekRange: WeekRange,
        aggregator: BrawlerPairBattleResultStatsAggregator,
    ): Long {
        var totalEntities = 0L
        transactionRunner.run { em, queryFactory ->
            queryFactory
                .delete(brawlerPairBattleResultStatsBackupEntity)
                .where(
                    brawlerPairBattleResultStatsBackupEntity.startBattleDate.eq(weekRange.start),
                    brawlerPairBattleResultStatsBackupEntity.endBattleDate.eq(weekRange.end),
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
