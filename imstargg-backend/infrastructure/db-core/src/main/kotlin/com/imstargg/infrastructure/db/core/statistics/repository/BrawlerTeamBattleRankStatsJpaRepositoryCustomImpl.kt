package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.RankStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamEmbedded
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsBrawlerEntity.Companion.brawlerTeamBattleRankStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsEntity.Companion.brawlerTeamBattleRankStatsEntity
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Predicate
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
            .where(
                stats.id.`in`(statsIdsWithBrawler),
                stats.eventBrawlStarsId.eq(eventBrawlStarsId),
                stats.startBattleDate.goe(startDate),
                stats.tierRange.eq(tierRange),
                stats.endBattleDate.loe(endDate),
                stats.brawlerTeam.teamSize.eq(teamSize),
            ).fetch()
    }

    override fun findPagedByConditions(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        brawlerBrawlStarsIds: List<Long>,
        teamSize: StatsTeamSize,
        sortBy: RankStatsSortBy,
        sortOrder: SortOrder,
        offset: Long,
        limit: Long,
    ): List<BrawlerTeamRankProjection> {
        val teamKeyExpr = brawlerTeamBattleRankStatsEntity.brawlerTeam.teamKey
        val totalCountSum = brawlerTeamBattleRankStatsEntity.rankCounts.totalCount.sumLong()
        val weightedRankSumExpr = brawlerTeamBattleRankStatsEntity.weightedRankSum.sumLong()

        val orderSpecifier =
            rankStatsOrderSpecifier(
                sortBy = sortBy,
                sortOrder = sortOrder,
                totalCountSum = totalCountSum,
                weightedRankSumSum = weightedRankSumExpr,
            )

        return queryFactory
            .select(teamKeyExpr, totalCountSum, weightedRankSumExpr)
            .from(brawlerTeamBattleRankStatsEntity)
            .where(
                brawlerIdFilters(brawlerBrawlStarsIds),
                brawlerTeamBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                brawlerTeamBattleRankStatsEntity.tierRange.eq(tierRange),
                brawlerTeamBattleRankStatsEntity.startBattleDate.goe(startDate),
                brawlerTeamBattleRankStatsEntity.endBattleDate.loe(endDate),
                brawlerTeamBattleRankStatsEntity.brawlerTeam.teamSize.eq(teamSize),
            ).groupBy(teamKeyExpr)
            .orderBy(orderSpecifier, teamKeyExpr.asc())
            .offset(offset)
            .limit(limit)
            .fetch()
            .map { tuple ->
                BrawlerTeamRankProjection(
                    brawlerTeam =
                        BrawlerTeamEmbedded(
                            teamKey = tuple[teamKeyExpr]!!,
                            teamSize = teamSize,
                        ),
                    totalCount = tuple[totalCountSum]!!,
                    weightedRankSum = tuple[weightedRankSumExpr]!!,
                )
            }
    }

    override fun countTotalBattleCount(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        brawlerBrawlStarsIds: List<Long>,
        teamSize: StatsTeamSize,
    ): Long =
        queryFactory
            .select(brawlerTeamBattleRankStatsEntity.rankCounts.totalCount.sumLong())
            .from(brawlerTeamBattleRankStatsEntity)
            .where(
                brawlerIdFilters(brawlerBrawlStarsIds),
                brawlerTeamBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                brawlerTeamBattleRankStatsEntity.tierRange.eq(tierRange),
                brawlerTeamBattleRankStatsEntity.startBattleDate.goe(startDate),
                brawlerTeamBattleRankStatsEntity.endBattleDate.loe(endDate),
                brawlerTeamBattleRankStatsEntity.brawlerTeam.teamSize.eq(teamSize),
            ).fetchOne() ?: 0L

    override fun countDistinctTeamKeys(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        brawlerBrawlStarsIds: List<Long>,
        teamSize: StatsTeamSize,
    ): Long =
        queryFactory
            .select(brawlerTeamBattleRankStatsEntity.brawlerTeam.teamKey.countDistinct())
            .from(brawlerTeamBattleRankStatsEntity)
            .where(
                brawlerIdFilters(brawlerBrawlStarsIds),
                brawlerTeamBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                brawlerTeamBattleRankStatsEntity.tierRange.eq(tierRange),
                brawlerTeamBattleRankStatsEntity.startBattleDate.goe(startDate),
                brawlerTeamBattleRankStatsEntity.endBattleDate.loe(endDate),
                brawlerTeamBattleRankStatsEntity.brawlerTeam.teamSize.eq(teamSize),
            ).fetchOne() ?: 0L

    private fun brawlerIdFilters(brawlerBrawlStarsIds: List<Long>): Predicate {
        val builder = BooleanBuilder()
        brawlerBrawlStarsIds.forEach { id ->
            builder.and(
                brawlerTeamBattleRankStatsEntity.id.`in`(
                    JPAExpressions
                        .select(brawlerTeamBattleRankStatsBrawlerEntity.stats.id)
                        .from(brawlerTeamBattleRankStatsBrawlerEntity)
                        .where(brawlerTeamBattleRankStatsBrawlerEntity.brawlerBrawlStarsId.eq(id)),
                ),
            )
        }
        return builder
    }

    private fun rankStatsOrderSpecifier(
        sortBy: RankStatsSortBy,
        sortOrder: SortOrder,
        totalCountSum: NumberExpression<Long>,
        weightedRankSumSum: NumberExpression<Long>,
    ): OrderSpecifier<*> {
        val expression =
            when (sortBy) {
                RankStatsSortBy.PICK_RATE -> totalCountSum
                RankStatsSortBy.AVERAGE_RANK ->
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
}
