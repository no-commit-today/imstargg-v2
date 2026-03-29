package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity.Companion.statsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager

internal class StatsResultBattleJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : StatsResultBattleJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllBattleKeysByStatsResultBattles(statsResultBattles: Collection<StatsResultBattleEntity>): Set<String> {
        if (statsResultBattles.isEmpty()) return emptySet()

        val battleKeys = statsResultBattles.map { it.battleKey }
        val battleDates = statsResultBattles.map { it.battleAt.toKoreaLocalDate() }.distinct()

        return queryFactory
            .select(statsResultBattleEntity.battleKey)
            .from(statsResultBattleEntity)
            .where(
                statsResultBattleEntity.battleDate.`in`(battleDates),
                statsResultBattleEntity.battleKey.`in`(battleKeys),
            ).fetch()
            .toSet()
    }
}
