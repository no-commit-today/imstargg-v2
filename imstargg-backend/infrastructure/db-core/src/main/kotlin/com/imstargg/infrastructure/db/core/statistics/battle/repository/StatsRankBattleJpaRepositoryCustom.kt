package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity

interface StatsRankBattleJpaRepositoryCustom {
    fun findAllBattleKeysByStatsRankBattles(statsRankBattles: Collection<StatsRankBattleEntity>): Set<String>
}
