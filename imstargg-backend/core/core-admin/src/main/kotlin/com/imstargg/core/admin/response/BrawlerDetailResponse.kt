package com.imstargg.core.admin.response

import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language

data class BrawlerDetailResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val imageUrl: String?,
    val buffies: List<BuffyResponse>,
    val starPowers: List<StarPowerResponse>,
    val gadgets: List<GadgetResponse>,
    val hyperCharges: List<HyperChargeResponse>,
    val linkedGearBrawlStarsIds: Set<Long>,
    val allGears: List<GearResponse>,
)
