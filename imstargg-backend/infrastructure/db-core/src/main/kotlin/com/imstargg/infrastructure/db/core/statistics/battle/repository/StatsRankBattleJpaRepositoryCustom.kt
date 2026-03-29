package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity

interface StatsRankBattleJpaRepositoryCustom {
    fun findAllBattleKeyAndPlayerTagByStatsRankBattles(statsRankBattles: Collection<StatsRankBattleEntity>): Set<Pair<String, String>>
}
