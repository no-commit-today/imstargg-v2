package com.imstargg.core.admin.controller.response

import com.imstargg.core.enums.Language

data class BattleEventModeResponse(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val names: Map<Language, String>,
    val hasIcon: Boolean,
)
