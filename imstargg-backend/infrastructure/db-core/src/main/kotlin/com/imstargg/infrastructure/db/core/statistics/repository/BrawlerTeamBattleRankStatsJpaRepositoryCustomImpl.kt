package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsBrawlerEntity.Companion.brawlerTeamBattleRankStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsEntity.Companion.brawlerTeamBattleRankStatsEntity
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerTeamBattleRankStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerTeamBattleRankStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllByEventAndConditionsAndBrawlerAndTeamSize(
        eventBrawlStarsId: Long,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        battleDate: LocalDate,
        brawlerBrawlStarsId: Long,
        teamSize: Int,
    ): List<BrawlerTeamBattleRankStatsEntity> {
        val stats = brawlerTeamBattleRankStatsEntity
        val brawler = brawlerTeamBattleRankStatsBrawlerEntity

        val statsIdsWithBrawler =
            JPAExpressions
                .select(brawler.stats.id)
                .from(brawler)
                .where(brawler.brawlerBrawlStarsId.eq(brawlerBrawlStarsId))

        return queryFactory
            .selectFrom(stats)
            .distinct()
            .leftJoin(stats.brawlers, brawler)
            .fetchJoin()
            .where(
                stats.id.`in`(statsIdsWithBrawler),
                stats.eventBrawlStarsId.eq(eventBrawlStarsId),
                stats.periodType.eq(periodType),
                stats.tierRange.eq(tierRange),
                stats.battleDate.eq(battleDate),
                stats.teamSize.eq(teamSize),
            ).fetch()
    }
}
