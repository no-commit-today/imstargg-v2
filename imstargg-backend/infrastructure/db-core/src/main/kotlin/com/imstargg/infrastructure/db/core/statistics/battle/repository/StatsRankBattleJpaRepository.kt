package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface StatsRankBattleJpaRepository :
    JpaRepository<StatsRankBattleEntity, Long>,
    StatsRankBattleJpaRepositoryCustom
