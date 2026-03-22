package com.imstargg.infrastructure.db.core.statistics.battle.repository

import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface StatsResultBattleJpaRepository : JpaRepository<StatsResultBattleEntity, Long>
