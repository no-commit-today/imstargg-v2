package com.imstargg.infrastructure.db.core.brawlstars.battle

import org.springframework.data.jpa.repository.JpaRepository

interface BattleTeamPlayerJpaRepository : JpaRepository<BattleTeamPlayerEntity, Long> {
    fun findAllByBattleIdIn(battleIds: List<Long>): List<BattleTeamPlayerEntity>

    fun findAllByBattle(battle: BattleEntity): List<BattleTeamPlayerEntity>
}
