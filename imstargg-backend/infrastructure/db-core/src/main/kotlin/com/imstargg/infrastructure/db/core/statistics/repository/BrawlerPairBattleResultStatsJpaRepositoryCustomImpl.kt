package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleResultStatsEntity.Companion.brawlerPairBattleResultStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerPairBattleResultStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerPairBattleResultStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllByConditions(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerPairResultProjection> {
        val brawlerId = brawlerPairBattleResultStatsEntity.brawlerBrawlStarsId
        val pairBrawlerId = brawlerPairBattleResultStatsEntity.pairBrawlerBrawlStarsId
        val victorySum = brawlerPairBattleResultStatsEntity.resultCount.victoryCount.sumLong()
        val defeatSum = brawlerPairBattleResultStatsEntity.resultCount.defeatCount.sumLong()
        val drawSum = brawlerPairBattleResultStatsEntity.resultCount.drawCount.sumLong()

        return queryFactory
            .select(brawlerId, pairBrawlerId, victorySum, defeatSum, drawSum)
            .from(brawlerPairBattleResultStatsEntity)
            .where(
                eventBrawlStarsId?.let { brawlerPairBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerPairBattleResultStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
                pairBrawlerBrawlStarsId?.let { brawlerPairBattleResultStatsEntity.pairBrawlerBrawlStarsId.eq(it) },
                brawlerPairBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerPairBattleResultStatsEntity.startBattleDate.goe(startDate),
                brawlerPairBattleResultStatsEntity.endBattleDate.loe(endDate),
            ).groupBy(brawlerId, pairBrawlerId)
            .orderBy(pairBrawlerId.asc())
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
}
