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

    override fun findEventTotalSlotSums(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<EventSlotSumProjection> {
        val eventId = brawlerBattleResultStatsEntity.eventBrawlStarsId
        val totalSlotSum =
            brawlerBattleResultStatsEntity.resultCount.victoryCount
                .sumLong()
                .add(brawlerBattleResultStatsEntity.resultCount.defeatCount.sumLong())
                .add(brawlerBattleResultStatsEntity.resultCount.drawCount.sumLong())

        return queryFactory
            .select(eventId, totalSlotSum)
            .from(brawlerBattleResultStatsEntity)
            .where(*conditions(eventBrawlStarsId, tierRange, startDate, endDate))
            .groupBy(eventId)
            .fetch()
            .map { tuple ->
                EventSlotSumProjection(
                    eventBrawlStarsId = tuple[eventId]!!,
                    totalSlotCount = tuple[totalSlotSum] ?: 0L,
                )
            }
    }

    override fun findGroupedByEventAndBrawler(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEventResultProjection> {
        val eventId = brawlerBattleResultStatsEntity.eventBrawlStarsId
        val brawlerId = brawlerBattleResultStatsEntity.brawlerBrawlStarsId
        val victorySum = brawlerBattleResultStatsEntity.resultCount.victoryCount.sumLong()
        val defeatSum = brawlerBattleResultStatsEntity.resultCount.defeatCount.sumLong()
        val drawSum = brawlerBattleResultStatsEntity.resultCount.drawCount.sumLong()
        val starPlayerSum = brawlerBattleResultStatsEntity.starPlayerCount.sumLong()

        return queryFactory
            .select(eventId, brawlerId, victorySum, defeatSum, drawSum, starPlayerSum)
            .from(brawlerBattleResultStatsEntity)
            .where(*conditions(eventBrawlStarsId, tierRange, startDate, endDate))
            .groupBy(eventId, brawlerId)
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

    private fun conditions(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ) = arrayOf(
        eventBrawlStarsId?.let { brawlerBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
        brawlerBattleResultStatsEntity.tierRange.eq(tierRange),
        brawlerBattleResultStatsEntity.battleDate.goe(startDate),
        brawlerBattleResultStatsEntity.battleDate.loe(endDate),
    )
}
