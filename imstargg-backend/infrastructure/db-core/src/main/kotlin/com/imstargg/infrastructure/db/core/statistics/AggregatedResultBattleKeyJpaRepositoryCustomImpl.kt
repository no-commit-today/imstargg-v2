package com.imstargg.infrastructure.db.core.statistics

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

        val entity = QAggregatedResultBattleKeyEntity.aggregatedResultBattleKeyEntity
        val battleKeys = statsResultBattles.map { it.battleKey }
        val battleDates = statsResultBattles.map { it.battleAt.toKoreaLocalDate() }.distinct()

        return queryFactory
            .selectFrom(entity)
            .where(
                entity.battleDate.`in`(battleDates),
                entity.battleKey.`in`(battleKeys),
            ).fetch()
    }
}
