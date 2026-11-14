package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTrioBattleResultStatsBackupEntity.Companion.brawlerTrioBattleResultStatsBackupEntity
import com.querydsl.core.Tuple
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerTrioBattleResultStatsBackupJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerTrioBattleResultStatsBackupJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    private val brawler1Id = brawlerTrioBattleResultStatsBackupEntity.brawler1BrawlStarsId
    private val brawler2Id = brawlerTrioBattleResultStatsBackupEntity.brawler2BrawlStarsId
    private val brawler3Id = brawlerTrioBattleResultStatsBackupEntity.brawler3BrawlStarsId
    private val victorySum = brawlerTrioBattleResultStatsBackupEntity.resultCount.victoryCount.sumLong()
    private val defeatSum = brawlerTrioBattleResultStatsBackupEntity.resultCount.defeatCount.sumLong()
    private val drawSum = brawlerTrioBattleResultStatsBackupEntity.resultCount.drawCount.sumLong()
    private val totalSum = brawlerTrioBattleResultStatsBackupEntity.resultCount.totalCount.sumLong()

    override fun findAllByBrawler(
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerTrioResultProjection> =
        queryFactory
            .select(brawler1Id, brawler2Id, brawler3Id, victorySum, defeatSum, drawSum)
            .from(brawlerTrioBattleResultStatsBackupEntity)
            .where(
                brawler1Id
                    .eq(brawlerBrawlStarsId)
                    .or(brawler2Id.eq(brawlerBrawlStarsId))
                    .or(brawler3Id.eq(brawlerBrawlStarsId)),
                brawlerTrioBattleResultStatsBackupEntity.tierRange.eq(tierRange),
                brawlerTrioBattleResultStatsBackupEntity.startBattleDate.between(startDate, endDate),
            ).groupBy(brawler1Id, brawler2Id, brawler3Id)
            .orderBy(brawler1Id.asc(), brawler2Id.asc(), brawler3Id.asc())
            .fetch()
            .map { it.toProjection() }

    override fun findAllHavingTotalCountAtLeast(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        minTotalCount: Long,
    ): List<BrawlerTrioResultProjection> =
        queryFactory
            .select(brawler1Id, brawler2Id, brawler3Id, victorySum, defeatSum, drawSum)
            .from(brawlerTrioBattleResultStatsBackupEntity)
            .where(
                brawlerTrioBattleResultStatsBackupEntity.tierRange.eq(tierRange),
                brawlerTrioBattleResultStatsBackupEntity.startBattleDate.between(startDate, endDate),
            ).groupBy(brawler1Id, brawler2Id, brawler3Id)
            .having(totalSum.goe(minTotalCount))
            .orderBy(brawler1Id.asc(), brawler2Id.asc(), brawler3Id.asc())
            .fetch()
            .map { it.toProjection() }

    override fun sumTotalCount(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long =
        queryFactory
            .select(totalSum)
            .from(brawlerTrioBattleResultStatsBackupEntity)
            .where(
                brawlerTrioBattleResultStatsBackupEntity.tierRange.eq(tierRange),
                brawlerTrioBattleResultStatsBackupEntity.startBattleDate.between(startDate, endDate),
            ).fetchOne() ?: 0L

    private fun Tuple.toProjection() =
        BrawlerTrioResultProjection(
            brawler1BrawlStarsId = this[brawler1Id]!!,
            brawler2BrawlStarsId = this[brawler2Id]!!,
            brawler3BrawlStarsId = this[brawler3Id]!!,
            victoryCount = this[victorySum]!!,
            defeatCount = this[defeatSum]!!,
            drawCount = this[drawSum]!!,
        )
}
