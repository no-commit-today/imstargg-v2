package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.AggregatedResultBattleKeyEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QAggregatedResultBattleKeyEntity.Companion.aggregatedResultBattleKeyEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager

internal class AggregatedResultBattleKeyJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : AggregatedResultBattleKeyJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllByStatsResultBattles(
        statsResultBattles: Collection<StatsResultBattleEntity>,
    ): List<AggregatedResultBattleKeyEntity> {
        if (statsResultBattles.isEmpty()) return emptyList()

        val battleKeys = statsResultBattles.map { it.battleKey }
        val battleDates = statsResultBattles.map { it.battleAt.toKoreaLocalDate() }.distinct()

        return queryFactory
            .selectFrom(aggregatedResultBattleKeyEntity)
            .where(
                aggregatedResultBattleKeyEntity.battleDate.`in`(battleDates),
                aggregatedResultBattleKeyEntity.battleKey.`in`(battleKeys),
            ).fetch()
    }
}
