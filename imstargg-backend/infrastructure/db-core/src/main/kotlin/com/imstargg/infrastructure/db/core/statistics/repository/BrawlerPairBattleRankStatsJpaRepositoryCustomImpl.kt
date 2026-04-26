package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.RankStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleRankStatsEntity.Companion.brawlerPairBattleRankStatsEntity
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerPairBattleRankStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerPairBattleRankStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findPagedByConditions(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        sortBy: RankStatsSortBy,
        sortOrder: SortOrder,
        offset: Long,
        limit: Long,
    ): List<BrawlerPairRankProjection> {
        val brawlerId = brawlerPairBattleRankStatsEntity.brawlerBrawlStarsId
        val pairBrawlerId = brawlerPairBattleRankStatsEntity.pairBrawlerBrawlStarsId
        val totalCountSum = brawlerPairBattleRankStatsEntity.rankCounts.totalCount.sumLong()
        val weightedRankSum = brawlerPairBattleRankStatsEntity.weightedRankSum.sumLong()

        val orderSpecifier =
            rankStatsOrderSpecifier(
                sortBy = sortBy,
                sortOrder = sortOrder,
                totalCountSum = totalCountSum,
                weightedRankSum = weightedRankSum,
            )

        return queryFactory
            .select(brawlerId, pairBrawlerId, totalCountSum, weightedRankSum)
            .from(brawlerPairBattleRankStatsEntity)
            .where(
                brawlerPairBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                brawlerPairBattleRankStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
                pairBrawlerBrawlStarsId?.let { brawlerPairBattleRankStatsEntity.pairBrawlerBrawlStarsId.eq(it) },
                brawlerPairBattleRankStatsEntity.tierRange.eq(tierRange),
                brawlerPairBattleRankStatsEntity.startBattleDate.goe(startDate),
                brawlerPairBattleRankStatsEntity.endBattleDate.loe(endDate),
            ).groupBy(brawlerId, pairBrawlerId)
            .orderBy(orderSpecifier, pairBrawlerId.asc())
            .offset(offset)
            .limit(limit)
            .fetch()
            .map { tuple ->
                BrawlerPairRankProjection(
                    brawlerBrawlStarsId = tuple[brawlerId]!!,
                    pairBrawlerBrawlStarsId = tuple[pairBrawlerId]!!,
                    totalCount = tuple[totalCountSum]!!,
                    weightedRankSum = tuple[weightedRankSum]!!,
                )
            }
    }

    override fun countTotalBattleCount(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        queryFactory
            .select(brawlerPairBattleRankStatsEntity.rankCounts.totalCount.sumLong())
            .from(brawlerPairBattleRankStatsEntity)
            .where(
                brawlerPairBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                brawlerPairBattleRankStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
                brawlerPairBattleRankStatsEntity.tierRange.eq(tierRange),
                brawlerPairBattleRankStatsEntity.startBattleDate.goe(startDate),
                brawlerPairBattleRankStatsEntity.endBattleDate.loe(endDate),
            ).fetchOne() ?: 0L

    override fun countDistinctPairBrawlers(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        queryFactory
            .select(brawlerPairBattleRankStatsEntity.pairBrawlerBrawlStarsId.countDistinct())
            .from(brawlerPairBattleRankStatsEntity)
            .where(
                brawlerPairBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                brawlerPairBattleRankStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
                pairBrawlerBrawlStarsId?.let { brawlerPairBattleRankStatsEntity.pairBrawlerBrawlStarsId.eq(it) },
                brawlerPairBattleRankStatsEntity.tierRange.eq(tierRange),
                brawlerPairBattleRankStatsEntity.startBattleDate.goe(startDate),
                brawlerPairBattleRankStatsEntity.endBattleDate.loe(endDate),
            ).fetchOne() ?: 0L

    private fun rankStatsOrderSpecifier(
        sortBy: RankStatsSortBy,
        sortOrder: SortOrder,
        totalCountSum: NumberExpression<Long>,
        weightedRankSum: NumberExpression<Long>,
    ): OrderSpecifier<*> {
        val expression =
            when (sortBy) {
                RankStatsSortBy.PICK_RATE -> totalCountSum
                RankStatsSortBy.AVERAGE_RANK ->
                    Expressions.numberTemplate(
                        Double::class.java,
                        "cast({0} as double) / nullif(cast({1} as double), 0.0)",
                        weightedRankSum,
                        totalCountSum,
                    )
            }
        return when (sortOrder) {
            SortOrder.ASC -> expression.asc()
            SortOrder.DESC -> expression.desc()
        }
    }
}
