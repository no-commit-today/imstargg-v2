package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsBackupEntity.Companion.brawlerEnemyBattleResultStatsBackupEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerEnemyBattleResultStatsBackupJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerEnemyBattleResultStatsBackupJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllByBrawler(
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEnemyResultProjection> {
        val brawlerId = brawlerEnemyBattleResultStatsBackupEntity.brawlerBrawlStarsId
        val enemyBrawlerId = brawlerEnemyBattleResultStatsBackupEntity.enemyBrawlerBrawlStarsId
        val victorySum = brawlerEnemyBattleResultStatsBackupEntity.resultCount.victoryCount.sumLong()
        val defeatSum = brawlerEnemyBattleResultStatsBackupEntity.resultCount.defeatCount.sumLong()
        val drawSum = brawlerEnemyBattleResultStatsBackupEntity.resultCount.drawCount.sumLong()

        return queryFactory
            .select(brawlerId, enemyBrawlerId, victorySum, defeatSum, drawSum)
            .from(brawlerEnemyBattleResultStatsBackupEntity)
            .where(
                brawlerId.eq(brawlerBrawlStarsId),
                brawlerEnemyBattleResultStatsBackupEntity.tierRange.eq(tierRange),
                brawlerEnemyBattleResultStatsBackupEntity.startBattleDate.between(startDate, endDate),
            ).groupBy(brawlerId, enemyBrawlerId)
            .orderBy(enemyBrawlerId.asc())
            .fetch()
            .map { tuple ->
                BrawlerEnemyResultProjection(
                    brawlerBrawlStarsId = tuple[brawlerId]!!,
                    enemyBrawlerBrawlStarsId = tuple[enemyBrawlerId]!!,
                    victoryCount = tuple[victorySum]!!,
                    defeatCount = tuple[defeatSum]!!,
                    drawCount = tuple[drawSum]!!,
                )
            }
    }
}
