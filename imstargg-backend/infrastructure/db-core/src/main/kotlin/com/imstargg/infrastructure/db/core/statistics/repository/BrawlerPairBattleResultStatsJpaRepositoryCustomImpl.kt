package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleResultStatsEntity.Companion.brawlerPairBattleResultStatsEntity
import com.querydsl.core.Tuple
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerPairBattleResultStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerPairBattleResultStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    private val brawler1Id = brawlerPairBattleResultStatsEntity.brawler1BrawlStarsId
    private val brawler2Id = brawlerPairBattleResultStatsEntity.brawler2BrawlStarsId
    private val victorySum = brawlerPairBattleResultStatsEntity.resultCount.victoryCount.sumLong()
    private val defeatSum = brawlerPairBattleResultStatsEntity.resultCount.defeatCount.sumLong()
    private val drawSum = brawlerPairBattleResultStatsEntity.resultCount.drawCount.sumLong()
    private val totalSum = brawlerPairBattleResultStatsEntity.resultCount.totalCount.sumLong()

    override fun findAllByBrawler(
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerPairResultProjection> =
        queryFactory
            .select(brawler1Id, brawler2Id, victorySum, defeatSum, drawSum)
            .from(brawlerPairBattleResultStatsEntity)
            .where(
                brawler1Id.eq(brawlerBrawlStarsId).or(brawler2Id.eq(brawlerBrawlStarsId)),
                brawlerPairBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerPairBattleResultStatsEntity.startBattleDate.between(startDate, endDate),
            ).groupBy(brawler1Id, brawler2Id)
            .orderBy(brawler1Id.asc(), brawler2Id.asc())
            .fetch()
            .map { it.toProjection() }

    override fun findAllHavingTotalCountAtLeast(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        minTotalCount: Long,
    ): List<BrawlerPairResultProjection> =
        queryFactory
            .select(brawler1Id, brawler2Id, victorySum, defeatSum, drawSum)
            .from(brawlerPairBattleResultStatsEntity)
            .where(
                brawlerPairBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerPairBattleResultStatsEntity.startBattleDate.between(startDate, endDate),
            ).groupBy(brawler1Id, brawler2Id)
            .having(totalSum.goe(minTotalCount))
            .orderBy(brawler1Id.asc(), brawler2Id.asc())
            .fetch()
            .map { it.toProjection() }

    override fun sumTotalCount(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        queryFactory
            .select(totalSum)
            .from(brawlerPairBattleResultStatsEntity)
            .where(
                brawlerPairBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerPairBattleResultStatsEntity.startBattleDate.between(startDate, endDate),
            ).fetchOne() ?: 0L

    private fun Tuple.toProjection() =
        BrawlerPairResultProjection(
            brawler1BrawlStarsId = this[brawler1Id]!!,
            brawler2BrawlStarsId = this[brawler2Id]!!,
            victoryCount = this[victorySum]!!,
            defeatCount = this[defeatSum]!!,
            drawCount = this[drawSum]!!,
        )
}
