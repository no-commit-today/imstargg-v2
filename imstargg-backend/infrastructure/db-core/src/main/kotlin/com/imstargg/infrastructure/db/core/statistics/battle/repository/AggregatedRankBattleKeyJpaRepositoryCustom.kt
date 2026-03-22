package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.AggregatedRankBattleKeyEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity

interface AggregatedRankBattleKeyJpaRepositoryCustom {
    fun findAllByStatsRankBattles(statsRankBattles: Collection<StatsRankBattleEntity>): List<AggregatedRankBattleKeyEntity>
}
