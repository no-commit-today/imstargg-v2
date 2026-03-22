package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.AggregatedResultBattleKeyEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

interface AggregatedResultBattleKeyJpaRepositoryCustom {
    fun findAllByStatsResultBattles(statsResultBattles: Collection<StatsResultBattleEntity>): List<AggregatedResultBattleKeyEntity>
}
