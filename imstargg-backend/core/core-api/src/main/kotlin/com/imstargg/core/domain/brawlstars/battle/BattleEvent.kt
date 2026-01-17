package com.imstargg.core.domain.brawlstars.battle

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.BattleEventMode

data class BattleEvent(
    val id: BrawlStarsId?,
    val mode: BattleEventMode?,
    val map: String?,
)
