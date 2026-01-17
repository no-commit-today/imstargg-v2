package com.imstargg.core.domain.brawlstars.brawler

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.enums.GearRarity

data class Gear(
    val id: BrawlStarsId,
    val rarity: GearRarity,
    val names: Messages,
    val imagePath: String?,
)
