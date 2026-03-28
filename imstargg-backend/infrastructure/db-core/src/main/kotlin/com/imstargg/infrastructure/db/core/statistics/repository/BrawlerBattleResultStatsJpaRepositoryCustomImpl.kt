package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerBattleResultStatsEntity.Companion.brawlerBattleResultStatsEntity
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.core.types.dsl.NumberPath
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerBattleResultStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerBattleResultStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun aggregateByBrawler(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerBattleResultAggregateProjection> {
        val entity = brawlerBattleResultStatsEntity
        val victorySum = entity.resultCount.victoryCount.longSum()
        val defeatSum = entity.resultCount.defeatCount.longSum()
        val drawSum = entity.resultCount.drawCount.longSum()
        val starPlayerSum = entity.starPlayerCount.longSum()

        return queryFactory
            .select(
                entity.brawlerBrawlStarsId,
                victorySum,
                defeatSum,
                drawSum,
                starPlayerSum,
            ).from(entity)
            .where(
                entity.tierRange.eq(tierRange),
                entity.battleDate.between(startDate, endDate),
            ).groupBy(entity.brawlerBrawlStarsId)
            .fetch()
            .map { tuple ->
                BrawlerBattleResultAggregateProjection(
                    brawlerBrawlStarsId = tuple.get(entity.brawlerBrawlStarsId)!!,
                    victoryCount = tuple.get(victorySum)!!,
                    defeatCount = tuple.get(defeatSum)!!,
                    drawCount = tuple.get(drawSum)!!,
                    starPlayerCount = tuple.get(starPlayerSum)!!,
                )
            }
    }

    override fun aggregateByDate(
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerBattleResultDailyAggregateProjection> {
        val entity = brawlerBattleResultStatsEntity
        val victorySum = entity.resultCount.victoryCount.longSum()
        val defeatSum = entity.resultCount.defeatCount.longSum()
        val drawSum = entity.resultCount.drawCount.longSum()
        val starPlayerSum = entity.starPlayerCount.longSum()

        return queryFactory
            .select(
                entity.battleDate,
                victorySum,
                defeatSum,
                drawSum,
                starPlayerSum,
            ).from(entity)
            .where(
                entity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
                entity.tierRange.eq(tierRange),
                entity.battleDate.between(startDate, endDate),
            ).groupBy(entity.battleDate)
            .fetch()
            .map { tuple ->
                BrawlerBattleResultDailyAggregateProjection(
                    battleDate = tuple.get(entity.battleDate)!!,
                    victoryCount = tuple.get(victorySum)!!,
                    defeatCount = tuple.get(defeatSum)!!,
                    drawCount = tuple.get(drawSum)!!,
                    starPlayerCount = tuple.get(starPlayerSum)!!,
                )
            }
    }

    private fun NumberPath<Long>.longSum(): NumberExpression<Long> =
        Expressions.numberOperation(Long::class.javaObjectType, com.querydsl.core.types.Ops.AggOps.SUM_AGG, this)
}
