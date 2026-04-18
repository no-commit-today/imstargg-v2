package com.imstargg.core.admin.response

import com.imstargg.core.enums.GearRarity
import com.imstargg.core.enums.Language

data class GearResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val rarity: GearRarity,
    val imageUrl: String?,
)
