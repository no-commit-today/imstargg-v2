package com.imstargg.infrastructure.db.core.brawlstars.battle

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class BattleEntityEvent(
    @Column(name = "event_brawlstars_id", updatable = false)
    val brawlStarsId: Long?,
    @Column(name = "event_mode", length = 65, updatable = false)
    val mode: String?,
    @Column(name = "event_mode_id", updatable = false)
    val modeId: Int?,
    @Column(name = "event_map", length = 65, updatable = false)
    val map: String?,
)
