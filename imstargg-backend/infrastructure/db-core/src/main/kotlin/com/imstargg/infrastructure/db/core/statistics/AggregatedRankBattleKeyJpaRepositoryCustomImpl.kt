package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.infrastructure.db.core.statistics.QAggregatedRankBattleKeyEntity.Companion.aggregatedRankBattleKeyEntity
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager

internal class AggregatedRankBattleKeyJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : AggregatedRankBattleKeyJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun findAllByStatsRankBattles(statsRankBattles: Collection<StatsRankBattleEntity>): List<AggregatedRankBattleKeyEntity> {
        if (statsRankBattles.isEmpty()) return emptyList()

        val battleKeys = statsRankBattles.map { it.battleKey }
        val battleDates = statsRankBattles.map { it.battleAt.toKoreaLocalDate() }.distinct()

        return queryFactory
            .selectFrom(aggregatedRankBattleKeyEntity)
            .where(
                aggregatedRankBattleKeyEntity.battleDate.`in`(battleDates),
                aggregatedRankBattleKeyEntity.battleKey.`in`(battleKeys),
            ).fetch()
    }
}
