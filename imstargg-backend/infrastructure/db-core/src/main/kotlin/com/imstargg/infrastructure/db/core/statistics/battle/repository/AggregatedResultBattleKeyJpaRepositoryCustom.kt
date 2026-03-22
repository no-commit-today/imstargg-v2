package com.imstargg.infrastructure.db.core.statistics.battle

interface AggregatedResultBattleKeyJpaRepositoryCustom {
    fun findAllByStatsResultBattles(statsResultBattles: Collection<StatsResultBattleEntity>): List<AggregatedResultBattleKeyEntity>
}
