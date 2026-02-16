package com.imstargg.core.worker.domain

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.toTeams

class BattleResult(
    val battle: BattleEntity,
    val teamPlayers: List<BattleTeamPlayerEntity>,
) {
    fun teams(): List<List<BattleTeamPlayerEntity>> = teamPlayers.toTeams()
}
