package com.imstargg.core.admin.request

import com.imstargg.core.enums.Language

data class BattleEventModeCreateRequest(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val names: Map<Language, String> = emptyMap(),
)
