package com.imstargg.core.admin.response

import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity

data class BattleEventModeResponse(
    val mode: BattleEventModeEntity,
    val names: Map<Language, String>,
    val icon: BrawlStarsImageEntity?,
)
