package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultWeeklyStatsEntity.Companion.brawlerTeamBattleResultWeeklyStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerTeamBattleResultWeeklyStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerTeamBattleResultWeeklyStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findByEventAndTierRangeAndStartDateAndBrawlerBrawlStarsId(
        eventBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerTeamBattleResultWeeklyStatsEntity> {
        val entity = brawlerTeamBattleResultWeeklyStatsEntity

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

    override fun findByTierRangeAndStartDateAndBrawlerBrawlStarsId(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        brawlerBrawlStarsId: Long,
    ): List<BrawlerTeamBattleResultWeeklyStatsEntity> {
        val entity = brawlerTeamBattleResultWeeklyStatsEntity

        return queryFactory
            .selectFrom(entity)
            .where(
                entity.tierRange.eq(tierRange),
                entity.startDate.eq(startDate),
                entity.brawlerBrawlStarsId1
                    .eq(brawlerBrawlStarsId)
                    .or(entity.brawlerBrawlStarsId2.eq(brawlerBrawlStarsId))
                    .or(entity.brawlerBrawlStarsId3.eq(brawlerBrawlStarsId)),
            ).fetch()
    }
}
