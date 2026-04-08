package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsBrawlerEntity.Companion.brawlerTeamBattleResultStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsEntity.Companion.brawlerTeamBattleResultStatsEntity
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.LocalDate

internal class BrawlerTeamBattleResultStatsJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BrawlerTeamBattleResultStatsJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllByConditionsAndBrawler(
        eventBrawlStarsId: Long?,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        startBattleDate: LocalDate,
        brawlerBrawlStarsId: Long,
        teamSize: StatsTeamSize,
    ): List<BrawlerTeamBattleResultStatsEntity> {
        val stats = brawlerTeamBattleResultStatsEntity
        val brawler = brawlerTeamBattleResultStatsBrawlerEntity

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
                eventBrawlStarsId?.let { stats.eventBrawlStarsId.eq(it) },
                stats.periodType.eq(periodType),
                stats.tierRange.eq(tierRange),
                stats.startBattleDate.eq(startBattleDate),
                stats.teamSize.eq(teamSize),
            ).fetch()
    }
}
