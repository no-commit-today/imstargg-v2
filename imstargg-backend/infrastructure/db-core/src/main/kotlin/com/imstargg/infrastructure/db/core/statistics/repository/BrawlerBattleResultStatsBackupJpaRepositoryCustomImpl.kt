package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerBattleResultStatsBackupEntity.Companion.brawlerBattleResultStatsBackupEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerBattleResultStatsBackupJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerBattleResultStatsBackupJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findGroupedByEventAndBrawler(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEventResultProjection> {
        val eventId = brawlerBattleResultStatsBackupEntity.eventBrawlStarsId
        val brawlerId = brawlerBattleResultStatsBackupEntity.brawlerBrawlStarsId
        val victorySum = brawlerBattleResultStatsBackupEntity.resultCount.victoryCount.sumLong()
        val defeatSum = brawlerBattleResultStatsBackupEntity.resultCount.defeatCount.sumLong()
        val drawSum = brawlerBattleResultStatsBackupEntity.resultCount.drawCount.sumLong()
        val starPlayerSum = brawlerBattleResultStatsBackupEntity.starPlayerCount.sumLong()

        return queryFactory
            .select(eventId, brawlerId, victorySum, defeatSum, drawSum, starPlayerSum)
            .from(brawlerBattleResultStatsBackupEntity)
            .where(
                eventBrawlStarsId?.let { brawlerBattleResultStatsBackupEntity.eventBrawlStarsId.eq(it) },
                brawlerBattleResultStatsBackupEntity.tierRange.eq(tierRange),
                brawlerBattleResultStatsBackupEntity.battleDate.goe(startDate),
                brawlerBattleResultStatsBackupEntity.battleDate.loe(endDate),
            ).groupBy(eventId, brawlerId)
            .fetch()
            .map { tuple ->
                BrawlerEventResultProjection(
                    eventBrawlStarsId = tuple[eventId]!!,
                    brawlerBrawlStarsId = tuple[brawlerId]!!,
                    victoryCount = tuple[victorySum] ?: 0L,
                    defeatCount = tuple[defeatSum] ?: 0L,
                    drawCount = tuple[drawSum] ?: 0L,
                    starPlayerCount = tuple[starPlayerSum] ?: 0L,
                )
            }
    }
}
