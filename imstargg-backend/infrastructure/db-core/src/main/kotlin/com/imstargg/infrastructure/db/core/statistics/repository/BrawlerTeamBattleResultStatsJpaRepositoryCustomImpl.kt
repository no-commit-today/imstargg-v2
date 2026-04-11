package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.ResultStatsSortField
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsBrawlerEntity.Companion.brawlerTeamBattleResultStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsEntity.Companion.brawlerTeamBattleResultStatsEntity
import com.querydsl.core.Tuple
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerTeamBattleResultStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerTeamBattleResultStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllByConditions(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        brawlerBrawlStarsId: Long?,
        teamSize: StatsTeamSize,
    ): List<BrawlerTeamBattleResultStatsEntity> {
        val brawlerFilter =
            brawlerBrawlStarsId?.let {
                val statsIdsWithBrawler =
                    JPAExpressions
                        .select(brawlerTeamBattleResultStatsBrawlerEntity.stats.id)
                        .from(brawlerTeamBattleResultStatsBrawlerEntity)
                        .where(brawlerTeamBattleResultStatsBrawlerEntity.brawlerBrawlStarsId.eq(it))
                brawlerTeamBattleResultStatsEntity.id.`in`(statsIdsWithBrawler)
            }

        return queryFactory
            .selectFrom(brawlerTeamBattleResultStatsEntity)
            .distinct()
            .leftJoin(brawlerTeamBattleResultStatsEntity.brawlers, brawlerTeamBattleResultStatsBrawlerEntity)
            .fetchJoin()
            .where(
                brawlerFilter,
                eventBrawlStarsId?.let { brawlerTeamBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerTeamBattleResultStatsEntity.periodType.`in`(StatsPeriodType.entries),
                brawlerTeamBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerTeamBattleResultStatsEntity.battleDate.between(startDate, endDate),
                brawlerTeamBattleResultStatsEntity.brawlerTeam.teamSize.eq(teamSize),
            ).fetch()
    }

    override fun findPagedByConditions(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        brawlerBrawlStarsId: Long,
        teamSize: StatsTeamSize,
        sortField: ResultStatsSortField,
        sortOrder: SortOrder,
        offset: Long,
        limit: Long,
    ): List<Tuple> {
        val teamKey = brawlerTeamBattleResultStatsEntity.brawlerTeam.teamKey
        val victorySum = sumOf(brawlerTeamBattleResultStatsEntity.resultCount.victoryCount)
        val defeatSum = sumOf(brawlerTeamBattleResultStatsEntity.resultCount.defeatCount)
        val drawSum = sumOf(brawlerTeamBattleResultStatsEntity.resultCount.drawCount)
        val totalCountSum = sumOf(brawlerTeamBattleResultStatsEntity.resultCount.totalCount)

        val statsIdsWithBrawler =
            JPAExpressions
                .select(brawlerTeamBattleResultStatsBrawlerEntity.stats.id)
                .from(brawlerTeamBattleResultStatsBrawlerEntity)
                .where(brawlerTeamBattleResultStatsBrawlerEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId))

        val orderSpecifier =
            resultStatsOrderSpecifier(
                sortField = sortField,
                sortOrder = sortOrder,
                victorySum = victorySum,
                totalCountSum = totalCountSum,
            )

        return queryFactory
            .select(teamKey, victorySum, defeatSum, drawSum, totalCountSum)
            .from(brawlerTeamBattleResultStatsEntity)
            .where(
                brawlerTeamBattleResultStatsEntity.id.`in`(statsIdsWithBrawler),
                eventBrawlStarsId?.let { brawlerTeamBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerTeamBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerTeamBattleResultStatsEntity.periodType.`in`(StatsPeriodType.entries),
                brawlerTeamBattleResultStatsEntity.battleDate.between(startDate, endDate),
                brawlerTeamBattleResultStatsEntity.brawlerTeam.teamSize.eq(teamSize),
            ).groupBy(teamKey)
            .orderBy(orderSpecifier, teamKey.asc())
            .offset(offset)
            .limit(limit)
            .fetch()
    }

    override fun countTotalBattleCount(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        brawlerBrawlStarsId: Long,
        teamSize: StatsTeamSize,
    ): Long {
        val statsIdsWithBrawler =
            JPAExpressions
                .select(brawlerTeamBattleResultStatsBrawlerEntity.stats.id)
                .from(brawlerTeamBattleResultStatsBrawlerEntity)
                .where(brawlerTeamBattleResultStatsBrawlerEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId))

        return queryFactory
            .select(sumOf(brawlerTeamBattleResultStatsEntity.resultCount.totalCount))
            .from(brawlerTeamBattleResultStatsEntity)
            .where(
                brawlerTeamBattleResultStatsEntity.id.`in`(statsIdsWithBrawler),
                eventBrawlStarsId?.let { brawlerTeamBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerTeamBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerTeamBattleResultStatsEntity.periodType.`in`(StatsPeriodType.entries),
                brawlerTeamBattleResultStatsEntity.battleDate.between(startDate, endDate),
                brawlerTeamBattleResultStatsEntity.brawlerTeam.teamSize.eq(teamSize),
            ).fetchOne() ?: 0L
    }

    private fun resultStatsOrderSpecifier(
        sortField: ResultStatsSortField,
        sortOrder: SortOrder,
        victorySum: NumberExpression<Long>,
        totalCountSum: NumberExpression<Long>,
    ): OrderSpecifier<*> {
        val expression =
            when (sortField) {
                ResultStatsSortField.PICK_RATE -> totalCountSum
                ResultStatsSortField.WIN_RATE ->
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
