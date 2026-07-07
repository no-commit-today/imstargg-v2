package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleRankStatsEntity.Companion.brawlerPairBattleRankStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerPairBattleRankStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerPairBattleRankStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllByConditions(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerPairRankProjection> {
        val brawlerId = brawlerPairBattleRankStatsEntity.brawlerBrawlStarsId
        val pairBrawlerId = brawlerPairBattleRankStatsEntity.pairBrawlerBrawlStarsId
        val totalCountSum = brawlerPairBattleRankStatsEntity.rankCounts.totalCount.sumLong()
        val weightedRankSum = brawlerPairBattleRankStatsEntity.weightedRankSum.sumLong()

        return queryFactory
            .select(brawlerId, pairBrawlerId, totalCountSum, weightedRankSum)
            .from(brawlerPairBattleRankStatsEntity)
            .where(
                *conditions(
                    eventBrawlStarsId = eventBrawlStarsId,
                    brawlerBrawlStarsId = brawlerBrawlStarsId,
                    pairBrawlerBrawlStarsId = pairBrawlerBrawlStarsId,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ),
            ).groupBy(brawlerId, pairBrawlerId)
            .orderBy(pairBrawlerId.asc())
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
                *conditions(
                    eventBrawlStarsId = eventBrawlStarsId,
                    brawlerBrawlStarsId = brawlerBrawlStarsId,
                    pairBrawlerBrawlStarsId = null,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ),
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
                *conditions(
                    eventBrawlStarsId = eventBrawlStarsId,
                    brawlerBrawlStarsId = brawlerBrawlStarsId,
                    pairBrawlerBrawlStarsId = pairBrawlerBrawlStarsId,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ),
            ).fetchOne() ?: 0L

    private fun conditions(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ) = arrayOf(
        brawlerPairBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
        brawlerPairBattleRankStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
        pairBrawlerBrawlStarsId?.let { brawlerPairBattleRankStatsEntity.pairBrawlerBrawlStarsId.eq(it) },
        brawlerPairBattleRankStatsEntity.tierRange.eq(tierRange),
        brawlerPairBattleRankStatsEntity.startBattleDate.goe(startDate),
        brawlerPairBattleRankStatsEntity.endBattleDate.loe(endDate),
    )
}
