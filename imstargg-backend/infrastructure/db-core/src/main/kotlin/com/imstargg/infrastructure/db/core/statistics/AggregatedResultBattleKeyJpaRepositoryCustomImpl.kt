package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.infrastructure.db.core.statistics.QAggregatedResultBattleKeyEntity.Companion.aggregatedResultBattleKeyEntity
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager

class AggregatedResultBattleKeyJpaRepositoryCustomImpl(
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
