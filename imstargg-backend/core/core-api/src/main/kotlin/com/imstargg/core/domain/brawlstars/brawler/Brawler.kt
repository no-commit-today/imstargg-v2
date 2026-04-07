package com.imstargg.core.domain.brawlstars.brawler

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole

data class Brawler(
    val id: BrawlStarsId,
    val name: Messages,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val gadgets: List<Gadget>,
    val gears: List<Gear>,
    val starPowers: List<StarPower>,
    val hyperCharges: List<HyperCharge>,
    val portraitImageUrl: String?,
    val buffies: List<Buffy>,
)
