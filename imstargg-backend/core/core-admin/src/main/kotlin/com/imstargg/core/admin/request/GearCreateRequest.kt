package com.imstargg.core.admin.request

import com.imstargg.core.enums.GearRarity

data class GearCreateRequest(
    val brawlStarsId: Long,
    val rarity: GearRarity,
)
