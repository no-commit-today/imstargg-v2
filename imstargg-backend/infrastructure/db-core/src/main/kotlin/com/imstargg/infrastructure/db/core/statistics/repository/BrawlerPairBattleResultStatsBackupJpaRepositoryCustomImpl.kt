package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleResultStatsBackupEntity.Companion.brawlerPairBattleResultStatsBackupEntity
import com.querydsl.core.Tuple
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerPairBattleResultStatsBackupJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerPairBattleResultStatsBackupJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    private val brawler1Id = brawlerPairBattleResultStatsBackupEntity.brawler1BrawlStarsId
    private val brawler2Id = brawlerPairBattleResultStatsBackupEntity.brawler2BrawlStarsId
    private val victorySum = brawlerPairBattleResultStatsBackupEntity.resultCount.victoryCount.sumLong()
    private val defeatSum = brawlerPairBattleResultStatsBackupEntity.resultCount.defeatCount.sumLong()
    private val drawSum = brawlerPairBattleResultStatsBackupEntity.resultCount.drawCount.sumLong()
    private val totalSum = brawlerPairBattleResultStatsBackupEntity.resultCount.totalCount.sumLong()

    override fun findAllByBrawler(
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerPairResultProjection> =
        queryFactory
            .select(brawler1Id, brawler2Id, victorySum, defeatSum, drawSum)
            .from(brawlerPairBattleResultStatsBackupEntity)
            .where(
                brawler1Id.eq(brawlerBrawlStarsId).or(brawler2Id.eq(brawlerBrawlStarsId)),
                brawlerPairBattleResultStatsBackupEntity.tierRange.eq(tierRange),
                brawlerPairBattleResultStatsBackupEntity.startBattleDate.between(startDate, endDate),
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
            .from(brawlerPairBattleResultStatsBackupEntity)
            .where(
                brawlerPairBattleResultStatsBackupEntity.tierRange.eq(tierRange),
                brawlerPairBattleResultStatsBackupEntity.startBattleDate.between(startDate, endDate),
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
            .from(brawlerPairBattleResultStatsBackupEntity)
            .where(
                brawlerPairBattleResultStatsBackupEntity.tierRange.eq(tierRange),
                brawlerPairBattleResultStatsBackupEntity.startBattleDate.between(startDate, endDate),
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
