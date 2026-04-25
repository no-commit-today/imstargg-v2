package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerBattleResultStatsEntity.Companion.brawlerBattleResultStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerBattleResultStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerBattleResultStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findGroupedByBrawler(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultProjection> {
        val brawlerId = brawlerBattleResultStatsEntity.brawlerBrawlStarsId
        val victorySum = brawlerBattleResultStatsEntity.resultCount.victoryCount.sumLong()
        val defeatSum = brawlerBattleResultStatsEntity.resultCount.defeatCount.sumLong()
        val drawSum = brawlerBattleResultStatsEntity.resultCount.drawCount.sumLong()
        val starPlayerSum = brawlerBattleResultStatsEntity.starPlayerCount.sumLong()

        return queryFactory
            .select(brawlerId, victorySum, defeatSum, drawSum, starPlayerSum)
            .from(brawlerBattleResultStatsEntity)
            .where(
                eventBrawlStarsId?.let { brawlerBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerBattleResultStatsEntity.battleDate.goe(startDate),
                brawlerBattleResultStatsEntity.battleDate.loe(endDate),
            ).groupBy(brawlerId)
            .fetch()
            .map { tuple ->
                BrawlerResultProjection(
                    brawlerBrawlStarsId = tuple[brawlerId]!!,
                    victoryCount = tuple[victorySum] ?: 0L,
                    defeatCount = tuple[defeatSum] ?: 0L,
                    drawCount = tuple[drawSum] ?: 0L,
                    starPlayerCount = tuple[starPlayerSum] ?: 0L,
                )
            }
    }
}
