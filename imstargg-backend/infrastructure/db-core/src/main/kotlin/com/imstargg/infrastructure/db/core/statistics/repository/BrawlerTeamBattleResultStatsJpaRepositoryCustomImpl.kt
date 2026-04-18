package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.ResultStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamEmbedded
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsBrawlerEntity.Companion.brawlerTeamBattleResultStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsEntity.Companion.brawlerTeamBattleResultStatsEntity
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Predicate
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
        brawlerBrawlStarsIds: List<Long>,
        teamSize: StatsTeamSize,
        sortBy: ResultStatsSortBy,
        sortOrder: SortOrder,
        offset: Long,
        limit: Long,
    ): List<BrawlerTeamResultProjection> {
        val teamKey = brawlerTeamBattleResultStatsEntity.brawlerTeam.teamKey
        val victorySum = brawlerTeamBattleResultStatsEntity.resultCount.victoryCount.sumLong()
        val defeatSum = brawlerTeamBattleResultStatsEntity.resultCount.defeatCount.sumLong()
        val drawSum = brawlerTeamBattleResultStatsEntity.resultCount.drawCount.sumLong()
        val totalCountSum = brawlerTeamBattleResultStatsEntity.resultCount.totalCount.sumLong()

        val orderSpecifier =
            resultStatsOrderSpecifier(
                sortBy = sortBy,
                sortOrder = sortOrder,
                victorySum = victorySum,
                totalCountSum = totalCountSum,
            )

        return queryFactory
            .select(teamKey, victorySum, defeatSum, drawSum, totalCountSum)
            .from(brawlerTeamBattleResultStatsEntity)
            .where(
                brawlerIdFilters(brawlerBrawlStarsIds),
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
            .map { tuple ->
                BrawlerTeamResultProjection(
                    brawlerTeam =
                        BrawlerTeamEmbedded(
                            teamKey = tuple[teamKey]!!,
                            teamSize = teamSize,
                        ),
                    victoryCount = tuple[victorySum]!!,
                    defeatCount = tuple[defeatSum]!!,
                    drawCount = tuple[drawSum]!!,
                )
            }
    }

    override fun countTotalBattleCount(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        brawlerBrawlStarsIds: List<Long>,
        teamSize: StatsTeamSize,
    ): Long =
        queryFactory
            .select(brawlerTeamBattleResultStatsEntity.resultCount.totalCount.sumLong())
            .from(brawlerTeamBattleResultStatsEntity)
            .where(
                brawlerIdFilters(brawlerBrawlStarsIds),
                eventBrawlStarsId?.let { brawlerTeamBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerTeamBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerTeamBattleResultStatsEntity.periodType.`in`(StatsPeriodType.entries),
                brawlerTeamBattleResultStatsEntity.battleDate.between(startDate, endDate),
                brawlerTeamBattleResultStatsEntity.brawlerTeam.teamSize.eq(teamSize),
            ).fetchOne() ?: 0L

    override fun countDistinctTeamKeys(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        brawlerBrawlStarsIds: List<Long>,
        teamSize: StatsTeamSize,
    ): Long =
        queryFactory
            .select(brawlerTeamBattleResultStatsEntity.brawlerTeam.teamKey.countDistinct())
            .from(brawlerTeamBattleResultStatsEntity)
            .where(
                brawlerIdFilters(brawlerBrawlStarsIds),
                eventBrawlStarsId?.let { brawlerTeamBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerTeamBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerTeamBattleResultStatsEntity.periodType.`in`(StatsPeriodType.entries),
                brawlerTeamBattleResultStatsEntity.battleDate.between(startDate, endDate),
                brawlerTeamBattleResultStatsEntity.brawlerTeam.teamSize.eq(teamSize),
            ).fetchOne() ?: 0L

    private fun brawlerIdFilters(brawlerBrawlStarsIds: List<Long>): Predicate {
        val builder = BooleanBuilder()
        brawlerBrawlStarsIds.forEach { id ->
            builder.and(
                brawlerTeamBattleResultStatsEntity.id.`in`(
                    JPAExpressions
                        .select(brawlerTeamBattleResultStatsBrawlerEntity.stats.id)
                        .from(brawlerTeamBattleResultStatsBrawlerEntity)
                        .where(brawlerTeamBattleResultStatsBrawlerEntity.brawlerBrawlStarsId.eq(id)),
                ),
            )
        }
        return builder
    }

    private fun resultStatsOrderSpecifier(
        sortBy: ResultStatsSortBy,
        sortOrder: SortOrder,
        victorySum: NumberExpression<Long>,
        totalCountSum: NumberExpression<Long>,
    ): OrderSpecifier<Long> {
        val expression =
            when (sortBy) {
                ResultStatsSortBy.PICK_RATE -> totalCountSum
                ResultStatsSortBy.WIN_RATE -> victorySum
            }
        return when (sortOrder) {
            SortOrder.ASC -> expression.asc()
            SortOrder.DESC -> expression.desc()
        }
    }
}
