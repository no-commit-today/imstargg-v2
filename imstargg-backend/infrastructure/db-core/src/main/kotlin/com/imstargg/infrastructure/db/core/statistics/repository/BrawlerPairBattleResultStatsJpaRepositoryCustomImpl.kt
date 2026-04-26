package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.ResultStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleResultStatsEntity.Companion.brawlerPairBattleResultStatsEntity
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerPairBattleResultStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerPairBattleResultStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findPagedByConditions(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        sortBy: ResultStatsSortBy,
        sortOrder: SortOrder,
        offset: Long,
        limit: Long,
    ): List<BrawlerPairResultProjection> {
        val brawlerId = brawlerPairBattleResultStatsEntity.brawlerBrawlStarsId
        val pairBrawlerId = brawlerPairBattleResultStatsEntity.pairBrawlerBrawlStarsId
        val victorySum = brawlerPairBattleResultStatsEntity.resultCount.victoryCount.sumLong()
        val defeatSum = brawlerPairBattleResultStatsEntity.resultCount.defeatCount.sumLong()
        val drawSum = brawlerPairBattleResultStatsEntity.resultCount.drawCount.sumLong()
        val totalCountSum = brawlerPairBattleResultStatsEntity.resultCount.totalCount.sumLong()

        val orderSpecifier =
            resultStatsOrderSpecifier(
                sortBy = sortBy,
                sortOrder = sortOrder,
                victorySum = victorySum,
                totalCountSum = totalCountSum,
            )

        return queryFactory
            .select(brawlerId, pairBrawlerId, victorySum, defeatSum, drawSum, totalCountSum)
            .from(brawlerPairBattleResultStatsEntity)
            .where(
                eventBrawlStarsId?.let { brawlerPairBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerPairBattleResultStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
                pairBrawlerBrawlStarsId?.let { brawlerPairBattleResultStatsEntity.pairBrawlerBrawlStarsId.eq(it) },
                brawlerPairBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerPairBattleResultStatsEntity.startBattleDate.goe(startDate),
                brawlerPairBattleResultStatsEntity.endBattleDate.loe(endDate),
            ).groupBy(brawlerId, pairBrawlerId)
            .orderBy(orderSpecifier, pairBrawlerId.asc())
            .offset(offset)
            .limit(limit)
            .fetch()
            .map { tuple ->
                BrawlerPairResultProjection(
                    brawlerBrawlStarsId = tuple[brawlerId]!!,
                    pairBrawlerBrawlStarsId = tuple[pairBrawlerId]!!,
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
            .select(brawlerPairBattleResultStatsEntity.resultCount.totalCount.sumLong())
            .from(brawlerPairBattleResultStatsEntity)
            .where(
                eventBrawlStarsId?.let { brawlerPairBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerPairBattleResultStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
                brawlerPairBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerPairBattleResultStatsEntity.startBattleDate.goe(startDate),
                brawlerPairBattleResultStatsEntity.endBattleDate.loe(endDate),
            ).fetchOne() ?: 0L

    override fun countDistinctPairBrawlers(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        queryFactory
            .select(brawlerPairBattleResultStatsEntity.pairBrawlerBrawlStarsId.countDistinct())
            .from(brawlerPairBattleResultStatsEntity)
            .where(
                eventBrawlStarsId?.let { brawlerPairBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerPairBattleResultStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
                pairBrawlerBrawlStarsId?.let { brawlerPairBattleResultStatsEntity.pairBrawlerBrawlStarsId.eq(it) },
                brawlerPairBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerPairBattleResultStatsEntity.startBattleDate.goe(startDate),
                brawlerPairBattleResultStatsEntity.endBattleDate.loe(endDate),
            ).fetchOne() ?: 0L

    private fun resultStatsOrderSpecifier(
        sortBy: ResultStatsSortBy,
        sortOrder: SortOrder,
        victorySum: NumberExpression<Long>,
        totalCountSum: NumberExpression<Long>,
    ): OrderSpecifier<*> {
        val expression =
            when (sortBy) {
                ResultStatsSortBy.PICK_RATE -> totalCountSum
                ResultStatsSortBy.WIN_RATE ->
                    Expressions.numberTemplate(
                        Double::class.java,
                        "cast({0} as double) / nullif(cast({1} as double), 0.0)",
                        victorySum,
                        totalCountSum,
                    )
            }
        return when (sortOrder) {
            SortOrder.ASC -> expression.asc()
            SortOrder.DESC -> expression.desc()
        }
    }
}
