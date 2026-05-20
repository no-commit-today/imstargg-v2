package com.imstargg.core.admin.response

import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.Language

data class BattleEventModeResponse(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val names: Map<Language, String>,
    val iconImageUrl: String?,
    val resultType: BattleEventModeResultType,
    val playerCounts: List<Int>,
)
