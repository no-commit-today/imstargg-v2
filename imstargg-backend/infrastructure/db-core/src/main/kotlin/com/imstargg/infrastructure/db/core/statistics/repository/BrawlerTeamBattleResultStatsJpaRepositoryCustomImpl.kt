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

    override fun findAllByConditions(
        eventBrawlStarsId: Long?,
        periodType: StatsPeriodType,
        tierRange: StatsTierRange,
        startBattleDate: LocalDate,
        brawlerBrawlStarsId: Long?,
        teamSize: StatsTeamSize,
    ): List<BrawlerTeamBattleResultStatsEntity> {

        val brawlerFilter =
            brawlerBrawlStarsId?.let {
                val statsIdsWithBrawler =
                    JPAExpressions
                        .select(brawlerTeamBattleResultStatsBrawlerEntity.stats.id)
                        .from(brawlerTeamBattleResultStatsBrawlerEntity)
                        .where(brawlerTeamBattleResultStatsBrawlerEntity.brawlerBrawlStarsId.eq(it))
                brawlerTeamBattleResultStatsEntity.id.`in`(statsIdsWithBrawler)
            }

        return queryFactory
            .selectFrom(brawlerTeamBattleResultStatsEntity)
            .distinct()
            .leftJoin(brawlerTeamBattleResultStatsEntity.brawlers, brawlerTeamBattleResultStatsBrawlerEntity)
            .fetchJoin()
            .where(
                brawlerFilter,
                eventBrawlStarsId?.let { brawlerTeamBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
                brawlerTeamBattleResultStatsEntity.periodType.eq(periodType),
                brawlerTeamBattleResultStatsEntity.tierRange.eq(tierRange),
                brawlerTeamBattleResultStatsEntity.startBattleDate.eq(startBattleDate),
                brawlerTeamBattleResultStatsEntity.teamSize.eq(teamSize),
            ).fetch()
    }
}
