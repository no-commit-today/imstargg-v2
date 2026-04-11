package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.ResultStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import com.querydsl.core.Tuple
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.Expressions
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
    ): List<Tuple> {
        val brawlerId = brawlerEnemyBattleResultStatsEntity.brawlerBrawlStarsId
        val enemyBrawlerId = brawlerEnemyBattleResultStatsEntity.enemyBrawlerBrawlStarsId
        val victorySum = sumOf(brawlerEnemyBattleResultStatsEntity.resultCount.victoryCount)
        val defeatSum = sumOf(brawlerEnemyBattleResultStatsEntity.resultCount.defeatCount)
        val drawSum = sumOf(brawlerEnemyBattleResultStatsEntity.resultCount.drawCount)
        val totalCountSum = sumOf(brawlerEnemyBattleResultStatsEntity.resultCount.totalCount)

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
                brawlerEnemyBattleResultStatsEntity.periodType.`in`(StatsPeriodType.entries),
                brawlerEnemyBattleResultStatsEntity.battleDate.between(startDate, endDate),
            ).groupBy(brawlerId, enemyBrawlerId)
            .orderBy(orderSpecifier, enemyBrawlerId.asc())
            .offset(offset)
            .limit(limit)
            .fetch()
    }

    override fun countTotalBattleCount(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        queryFactory
            .select(sumOf(brawlerEnemyBattleResultStatsEntity.resultCount.totalCount))
            .from(brawlerEnemyBattleResultStatsEntity)
            .where(
                eventBrawlStarsId?.let { brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerEnemyBattleResultStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
                brawlerEnemyBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerEnemyBattleResultStatsEntity.periodType.`in`(StatsPeriodType.entries),
                brawlerEnemyBattleResultStatsEntity.battleDate.between(startDate, endDate),
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
                brawlerEnemyBattleResultStatsEntity.periodType.`in`(StatsPeriodType.entries),
                brawlerEnemyBattleResultStatsEntity.battleDate.between(startDate, endDate),
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
                ResultStatsSortBy.WIN_RATE ->
                    Expressions.numberTemplate(
                        Double::class.java,
                        "cast({0} as double) / cast({1} as double)",
                        victorySum,
                        totalCountSum,
                    )
            }
        return when (sortOrder) {
            SortOrder.ASC -> expression.asc()
            SortOrder.DESC -> expression.desc()
        }
    }

    private fun sumOf(path: com.querydsl.core.types.dsl.NumberPath<Long>): NumberExpression<Long> =
        Expressions.numberTemplate(Long::class.javaObjectType, "sum({0})", path)
}
