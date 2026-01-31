package com.imstargg.infrastructure.db.core.statistics

interface AggregatedRankBattleKeyJpaRepositoryCustom {
    fun findAllByStatsRankBattles(statsRankBattles: Collection<StatsRankBattleEntity>): List<AggregatedRankBattleKeyEntity>
}
