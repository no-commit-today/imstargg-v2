package com.imstargg.infrastructure.db.core.brawlstars.battle

import org.springframework.data.jpa.repository.JpaRepository

interface BattleJpaRepository : JpaRepository<BattleEntity, Long> {
    fun findTopByPlayerPlayerIdOrderByBattleTimeDesc(playerId: Long): BattleEntity?
}
