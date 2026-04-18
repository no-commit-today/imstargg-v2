package com.imstargg.core.admin.response

import com.imstargg.core.enums.Language

data class BattleEventResponse(
    val brawlStarsId: Long,
    val modeBrawlStarsName: String?,
    val mapName: String?,
    val mapNames: Map<Language, String>,
)
