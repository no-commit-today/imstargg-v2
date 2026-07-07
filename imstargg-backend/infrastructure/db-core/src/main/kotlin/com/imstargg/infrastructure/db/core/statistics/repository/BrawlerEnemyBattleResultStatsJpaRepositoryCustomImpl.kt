package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerEnemyBattleResultStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerEnemyBattleResultStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllByConditions(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEnemyResultProjection> {
        val brawlerId = brawlerEnemyBattleResultStatsEntity.brawlerBrawlStarsId
        val enemyBrawlerId = brawlerEnemyBattleResultStatsEntity.enemyBrawlerBrawlStarsId
        val victorySum = brawlerEnemyBattleResultStatsEntity.resultCount.victoryCount.sumLong()
        val defeatSum = brawlerEnemyBattleResultStatsEntity.resultCount.defeatCount.sumLong()
        val drawSum = brawlerEnemyBattleResultStatsEntity.resultCount.drawCount.sumLong()

        return queryFactory
            .select(brawlerId, enemyBrawlerId, victorySum, defeatSum, drawSum)
            .from(brawlerEnemyBattleResultStatsEntity)
            .where(*conditions(eventBrawlStarsId, brawlerBrawlStarsId, tierRange, startDate, endDate))
            .groupBy(brawlerId, enemyBrawlerId)
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

    override fun countDistinctEnemyBrawlers(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        queryFactory
            .select(brawlerEnemyBattleResultStatsEntity.enemyBrawlerBrawlStarsId.countDistinct())
            .from(brawlerEnemyBattleResultStatsEntity)
            .where(*conditions(eventBrawlStarsId, brawlerBrawlStarsId, tierRange, startDate, endDate))
            .fetchOne() ?: 0L

    private fun conditions(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ) = arrayOf(
        eventBrawlStarsId?.let { brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
        brawlerEnemyBattleResultStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
        brawlerEnemyBattleResultStatsEntity.tierRange.eq(tierRange),
        brawlerEnemyBattleResultStatsEntity.startBattleDate.goe(startDate),
        brawlerEnemyBattleResultStatsEntity.endBattleDate.loe(endDate),
    )
}
