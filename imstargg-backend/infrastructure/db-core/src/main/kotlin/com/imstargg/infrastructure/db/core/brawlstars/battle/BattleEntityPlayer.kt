package com.imstargg.infrastructure.db.core.brawlstars.battle

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class BattleEntityPlayer(
    @Column(name = "player_id", updatable = false, nullable = false)
    val playerId: Long,
    @Column(name = "battle_rank", updatable = false)
    val rank: Int?,
    @Column(name = "trophy_change", updatable = false)
    val trophyChange: Int?,
)
