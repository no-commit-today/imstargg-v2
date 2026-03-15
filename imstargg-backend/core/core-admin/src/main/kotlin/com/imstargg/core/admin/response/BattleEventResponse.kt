package com.imstargg.core.admin.response

import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity

data class BattleEventResponse(
    val event: BattleEventEntity,
    val mapNames: Map<Language, String>,
)
