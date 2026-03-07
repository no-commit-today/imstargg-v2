package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QStatsWeeklyBrawlerTrioBattleRankEntity.Companion.statsWeeklyBrawlerTrioBattleRankEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class StatsWeeklyBrawlerTrioBattleRankJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : StatsWeeklyBrawlerTrioBattleRankJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findByEventAndTierRangeAndStartDateAndBrawlerBrawlStarsId(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<StatsWeeklyBrawlerTrioBattleRankEntity> {
        val entity = statsWeeklyBrawlerTrioBattleRankEntity

        return queryFactory
            .selectFrom(entity)
            .where(
                entity.eventBrawlStarsId.eq(eventBrawlStarsId),
                entity.tierRange.eq(tierRange),
                entity.startDate.eq(startDate),
                entity.brawlerBrawlStarsId1
                    .eq(brawlerBrawlStarsId)
                    .or(entity.brawlerBrawlStarsId2.eq(brawlerBrawlStarsId))
                    .or(entity.brawlerBrawlStarsId3.eq(brawlerBrawlStarsId)),
            ).fetch()
    }
}
