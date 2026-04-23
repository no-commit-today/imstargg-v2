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

    override fun findAllBattleKeysByStatsRankBattles(statsRankBattles: Collection<StatsRankBattleEntity>): Set<String> {
        if (statsRankBattles.isEmpty()) return emptySet()

        val battleKeys = statsRankBattles.map { it.battleKey }
        val battleDates = statsRankBattles.map { it.battleAt.toKoreaLocalDate() }.distinct()

        return queryFactory
            .select(statsRankBattleEntity.battleKey)
            .from(statsRankBattleEntity)
            .where(
                statsRankBattleEntity.battleDate.`in`(battleDates),
                statsRankBattleEntity.battleKey.`in`(battleKeys),
            ).fetch()
            .toSet()
    }
}
