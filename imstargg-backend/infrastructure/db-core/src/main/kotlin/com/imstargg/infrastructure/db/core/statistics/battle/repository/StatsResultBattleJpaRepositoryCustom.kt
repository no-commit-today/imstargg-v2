package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

interface StatsResultBattleJpaRepositoryCustom {
    fun findAllBattleKeysByStatsResultBattles(statsResultBattles: Collection<StatsResultBattleEntity>): Set<String>
}
