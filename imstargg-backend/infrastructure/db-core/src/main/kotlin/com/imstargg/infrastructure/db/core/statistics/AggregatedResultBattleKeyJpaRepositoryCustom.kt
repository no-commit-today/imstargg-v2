package com.imstargg.infrastructure.db.core.statistics

interface AggregatedResultBattleKeyJpaRepositoryCustom {
    fun findAllByStatsResultBattles(statsResultBattles: Collection<StatsResultBattleEntity>): List<AggregatedResultBattleKeyEntity>
}
