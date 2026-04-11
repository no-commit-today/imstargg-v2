package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.RankStatsSortField
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsBrawlerEntity.Companion.brawlerTeamBattleRankStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsEntity.Companion.brawlerTeamBattleRankStatsEntity
import com.querydsl.core.Tuple
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerTeamBattleRankStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerTeamBattleRankStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllByEventAndConditionsAndBrawlerAndTeamSize(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        brawlerBrawlStarsId: Long,
        teamSize: StatsTeamSize,
    ): List<BrawlerTeamBattleRankStatsEntity> {
        val stats = brawlerTeamBattleRankStatsEntity
        val brawler = brawlerTeamBattleRankStatsBrawlerEntity

        val statsIdsWithBrawler =
            JPAExpressions
                .select(brawler.stats.id)
                .from(brawler)
                .where(brawler.brawlerBrawlStarsId.eq(brawlerBrawlStarsId))

        return queryFactory
            .selectFrom(stats)
            .distinct()
            .leftJoin(stats.brawlers, brawler)
            .fetchJoin()
            .where(
                stats.id.`in`(statsIdsWithBrawler),
                stats.eventBrawlStarsId.eq(eventBrawlStarsId),
                stats.periodType.`in`(StatsPeriodType.entries),
                stats.tierRange.eq(tierRange),
                stats.battleDate.between(startDate, endDate),
                stats.brawlerTeam.teamSize.eq(teamSize),
            ).fetch()
    }

    override fun findPagedByConditions(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        brawlerBrawlStarsId: Long,
        teamSize: StatsTeamSize,
        sortField: RankStatsSortField,
        sortOrder: SortOrder,
        offset: Long,
        limit: Long,
    ): List<Tuple> {
        val teamKeyExpr = brawlerTeamBattleRankStatsEntity.brawlerTeam.teamKey
        val totalCountSum = sumOf(brawlerTeamBattleRankStatsEntity.rankCounts.totalCount)
        val weightedRankSumExpr = sumOf(brawlerTeamBattleRankStatsEntity.weightedRankSum)

        val statsIdsWithBrawler =
            JPAExpressions
                .select(brawlerTeamBattleRankStatsBrawlerEntity.stats.id)
                .from(brawlerTeamBattleRankStatsBrawlerEntity)
                .where(brawlerTeamBattleRankStatsBrawlerEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId))

        val orderSpecifier =
            rankStatsOrderSpecifier(
                sortField = sortField,
                sortOrder = sortOrder,
                totalCountSum = totalCountSum,
                weightedRankSumSum = weightedRankSumExpr,
            )

        return queryFactory
            .select(teamKeyExpr, totalCountSum, weightedRankSumExpr)
            .from(brawlerTeamBattleRankStatsEntity)
            .where(
                brawlerTeamBattleRankStatsEntity.id.`in`(statsIdsWithBrawler),
                brawlerTeamBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                brawlerTeamBattleRankStatsEntity.tierRange.eq(tierRange),
                brawlerTeamBattleRankStatsEntity.periodType.`in`(StatsPeriodType.entries),
                brawlerTeamBattleRankStatsEntity.battleDate.between(startDate, endDate),
                brawlerTeamBattleRankStatsEntity.brawlerTeam.teamSize.eq(teamSize),
            ).groupBy(teamKeyExpr)
            .orderBy(orderSpecifier, teamKeyExpr.asc())
            .offset(offset)
            .limit(limit)
            .fetch()
    }

    override fun countTotalBattleCount(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        brawlerBrawlStarsId: Long,
        teamSize: StatsTeamSize,
    ): Long {
        val statsIdsWithBrawler =
            JPAExpressions
                .select(brawlerTeamBattleRankStatsBrawlerEntity.stats.id)
                .from(brawlerTeamBattleRankStatsBrawlerEntity)
                .where(brawlerTeamBattleRankStatsBrawlerEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId))

        return queryFactory
            .select(sumOf(brawlerTeamBattleRankStatsEntity.rankCounts.totalCount))
            .from(brawlerTeamBattleRankStatsEntity)
            .where(
                brawlerTeamBattleRankStatsEntity.id.`in`(statsIdsWithBrawler),
                brawlerTeamBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                brawlerTeamBattleRankStatsEntity.tierRange.eq(tierRange),
                brawlerTeamBattleRankStatsEntity.periodType.`in`(StatsPeriodType.entries),
                brawlerTeamBattleRankStatsEntity.battleDate.between(startDate, endDate),
                brawlerTeamBattleRankStatsEntity.brawlerTeam.teamSize.eq(teamSize),
            ).fetchOne() ?: 0L
    }

    private fun rankStatsOrderSpecifier(
        sortField: RankStatsSortField,
        sortOrder: SortOrder,
        totalCountSum: NumberExpression<Long>,
        weightedRankSumSum: NumberExpression<Long>,
    ): OrderSpecifier<*> {
        val expression =
            when (sortField) {
                RankStatsSortField.PICK_RATE -> totalCountSum
                RankStatsSortField.AVERAGE_RANK ->
                    Expressions.numberTemplate(
                        Double::class.java,
                        "cast({0} as double) / cast({1} as double)",
                        weightedRankSumSum,
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
