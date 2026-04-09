package com.imstargg.core.admin.controller.request

import com.imstargg.core.enums.GearRarity

data class GearCreateRequest(
    val brawlStarsId: Long,
    val rarity: GearRarity,
)
