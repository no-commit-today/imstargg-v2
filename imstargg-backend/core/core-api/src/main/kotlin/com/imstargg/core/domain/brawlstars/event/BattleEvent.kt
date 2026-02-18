package com.imstargg.core.domain.brawlstars.event

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages

data class BattleEvent(
    val id: BrawlStarsId,
    val mode: BattleEventMode?,
    val mapName: Messages,
)
