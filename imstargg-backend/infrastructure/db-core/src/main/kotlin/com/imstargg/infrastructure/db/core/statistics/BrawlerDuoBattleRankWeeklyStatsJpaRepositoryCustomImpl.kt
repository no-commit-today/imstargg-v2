package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerDuoBattleRankWeeklyStatsEntity.Companion.brawlerDuoBattleRankWeeklyStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerDuoBattleRankWeeklyStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerDuoBattleRankWeeklyStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findByEventAndTierRangeAndDateAndBrawlerBrawlStarsId(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        date: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerDuoBattleRankWeeklyStatsEntity> {
        val entity = brawlerDuoBattleRankWeeklyStatsEntity

        return queryFactory
            .selectFrom(entity)
            .where(
                entity.eventBrawlStarsId.eq(eventBrawlStarsId),
                entity.tierRange.eq(tierRange),
                entity.date.eq(date),
                entity.brawlerBrawlStarsId1
                    .eq(brawlerBrawlStarsId)
                    .or(entity.brawlerBrawlStarsId2.eq(brawlerBrawlStarsId)),
            ).fetch()
    }
}
