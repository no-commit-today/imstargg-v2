package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity.Companion.statsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager

internal class StatsRankBattleJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : StatsRankBattleJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllBattleKeyAndPlayerTagByStatsRankBattles(
        statsRankBattles: Collection<StatsRankBattleEntity>,
    ): Set<BattleKeyPlayerTagProjection> {
        if (statsRankBattles.isEmpty()) return emptySet()

        val battleKeys = statsRankBattles.map { it.battleKey }
        val battleDates = statsRankBattles.map { it.battleAt.toKoreaLocalDate() }.distinct()

        return queryFactory
            .select(statsRankBattleEntity.battleKey, statsRankBattleEntity.playerBrawlStarsTag)
            .from(statsRankBattleEntity)
            .where(
                statsRankBattleEntity.battleDate.`in`(battleDates),
                statsRankBattleEntity.battleKey.`in`(battleKeys),
            ).fetch()
            .map { tuple ->
                BattleKeyPlayerTagProjection(
                    battleKey = tuple[statsRankBattleEntity.battleKey]!!,
                    playerBrawlStarsTag = tuple[statsRankBattleEntity.playerBrawlStarsTag]!!,
                )
            }.toSet()
    }
}
