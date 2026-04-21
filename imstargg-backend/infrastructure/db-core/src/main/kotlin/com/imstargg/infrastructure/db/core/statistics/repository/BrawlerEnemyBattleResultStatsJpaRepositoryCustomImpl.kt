package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.ResultStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerEnemyBattleResultStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerEnemyBattleResultStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findPagedByConditions(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        sortField: ResultStatsSortBy,
        sortOrder: SortOrder,
        offset: Long,
        limit: Long,
    ): List<BrawlerEnemyResultProjection> {
        val brawlerId = brawlerEnemyBattleResultStatsEntity.brawlerBrawlStarsId
        val enemyBrawlerId = brawlerEnemyBattleResultStatsEntity.enemyBrawlerBrawlStarsId
        val victorySum = brawlerEnemyBattleResultStatsEntity.resultCount.victoryCount.sumLong()
        val defeatSum = brawlerEnemyBattleResultStatsEntity.resultCount.defeatCount.sumLong()
        val drawSum = brawlerEnemyBattleResultStatsEntity.resultCount.drawCount.sumLong()
        val totalCountSum = brawlerEnemyBattleResultStatsEntity.resultCount.totalCount.sumLong()

        val orderSpecifier =
            resultStatsOrderSpecifier(
                sortField = sortField,
                sortOrder = sortOrder,
                victorySum = victorySum,
                totalCountSum = totalCountSum,
            )

        return queryFactory
            .select(brawlerId, enemyBrawlerId, victorySum, defeatSum, drawSum, totalCountSum)
            .from(brawlerEnemyBattleResultStatsEntity)
            .where(
                eventBrawlStarsId?.let { brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerEnemyBattleResultStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
                brawlerEnemyBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerEnemyBattleResultStatsEntity.startBattleDate.goe(startDate),
                brawlerEnemyBattleResultStatsEntity.endBattleDate.loe(endDate),
            ).groupBy(brawlerId, enemyBrawlerId)
            .orderBy(orderSpecifier, enemyBrawlerId.asc())
            .offset(offset)
            .limit(limit)
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

    override fun countTotalBattleCount(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        queryFactory
            .select(brawlerEnemyBattleResultStatsEntity.resultCount.totalCount.sumLong())
            .from(brawlerEnemyBattleResultStatsEntity)
            .where(
                eventBrawlStarsId?.let { brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerEnemyBattleResultStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
                brawlerEnemyBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerEnemyBattleResultStatsEntity.startBattleDate.goe(startDate),
                brawlerEnemyBattleResultStatsEntity.endBattleDate.loe(endDate),
            ).fetchOne() ?: 0L

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
            .where(
                eventBrawlStarsId?.let { brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerEnemyBattleResultStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
                brawlerEnemyBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerEnemyBattleResultStatsEntity.startBattleDate.goe(startDate),
                brawlerEnemyBattleResultStatsEntity.endBattleDate.loe(endDate),
            ).fetchOne() ?: 0L

    private fun resultStatsOrderSpecifier(
        sortField: ResultStatsSortBy,
        sortOrder: SortOrder,
        victorySum: NumberExpression<Long>,
        totalCountSum: NumberExpression<Long>,
    ): OrderSpecifier<*> {
        val expression =
            when (sortField) {
                ResultStatsSortBy.PICK_RATE -> totalCountSum
                ResultStatsSortBy.WIN_RATE -> victorySum
            }
        return when (sortOrder) {
            SortOrder.ASC -> expression.asc()
            SortOrder.DESC -> expression.desc()
        }
    }
}
