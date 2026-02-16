package com.imstargg.infrastructure.db.core.brawlstars.battle

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class BattleTeamPlayerEntityBrawler(
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
    @Column(name = "brawler_name", length = 65, updatable = false, nullable = false)
    val name: String,
    @Column(name = "brawler_power", updatable = false, nullable = false)
    val power: Int,
    @Column(name = "brawler_trophies", updatable = false, nullable = false)
    val trophies: Int,
    @Column(name = "brawler_trophy_change", updatable = false)
    val trophyChange: Int?,
)
