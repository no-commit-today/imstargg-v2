package com.imstargg.core.admin.request

import com.imstargg.core.enums.Language

data class BattleEventCreateRequest(
    val brawlStarsId: Long,
    val modeId: Long?,
    val mapName: String?,
    val mapNames: Map<Language, String> = emptyMap(),
)
