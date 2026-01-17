package com.imstargg.core.domain.brawlstars.event

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.enums.BattleEventMode

data class Event(
    val id: BrawlStarsId,
    val mode: BattleEventMode?,
    val mapName: Messages,
)
