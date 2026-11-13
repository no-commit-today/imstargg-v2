package com.imstargg.core.domain.brawlstars.event

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages

data class BattleEventMode(
    val id: BrawlStarsId,
    val name: Messages,
    val iconImageUrl: String?,
)
