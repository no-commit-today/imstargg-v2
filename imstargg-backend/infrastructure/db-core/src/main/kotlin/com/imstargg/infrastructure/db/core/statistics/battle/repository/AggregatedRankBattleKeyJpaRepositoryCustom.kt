package com.imstargg.infrastructure.db.core.statistics.battle

interface AggregatedRankBattleKeyJpaRepositoryCustom {
    fun findAllByStatsRankBattles(statsRankBattles: Collection<StatsRankBattleEntity>): List<AggregatedRankBattleKeyEntity>
}
