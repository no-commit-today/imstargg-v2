package com.imstargg.core.batch.job.statistics.enemybattleresult

import com.imstargg.core.batch.domain.statistics.BrawlerEnemyBattleResultStatsAggregator
import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsBackupEntity.Companion.brawlerEnemyBattleResultStatsBackupEntity
import org.springframework.stereotype.Component

/**
 * 주 버킷 하나를 한 트랜잭션(REQUIRES_NEW)에서 통째로 교체한다 — 버킷의 기존 행을 지우고 집계 결과를 chunk 단위로 넣는다.
 * 다른 버킷의 행은 건드리지 않는다.
 */
@Component
class BrawlerEnemyBattleResultStatsWriter(
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun write(
        weekRange: WeekRange,
        aggregator: BrawlerEnemyBattleResultStatsAggregator,
    ): Long {
        var totalEntities = 0L
        transactionRunner.run { em, queryFactory ->
            queryFactory
                .delete(brawlerEnemyBattleResultStatsBackupEntity)
                .where(
                    brawlerEnemyBattleResultStatsBackupEntity.startBattleDate.eq(weekRange.start),
                    brawlerEnemyBattleResultStatsBackupEntity.endBattleDate.eq(weekRange.end),
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
