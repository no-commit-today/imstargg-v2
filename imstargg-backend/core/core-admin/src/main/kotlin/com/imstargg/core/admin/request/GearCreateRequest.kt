package com.imstargg.core.admin.request

import com.imstargg.core.enums.GearRarity
import com.imstargg.core.enums.Language

data class GearCreateRequest(
    val brawlStarsId: Long,
    val rarity: GearRarity,
    val names: Map<Language, String> = emptyMap(),
)
