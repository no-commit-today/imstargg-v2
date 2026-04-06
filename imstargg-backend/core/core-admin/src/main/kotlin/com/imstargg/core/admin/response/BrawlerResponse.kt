package com.imstargg.core.admin.response

import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerEntity

data class BrawlerResponse(
    val brawler: BrawlerEntity,
    val names: Map<Language, String>,
    val image: BrawlStarsImageEntity?,
    val starPowers: List<StarPowerResponse> = emptyList(),
    val gadgets: List<GadgetResponse> = emptyList(),
    val hyperCharges: List<HyperChargeResponse> = emptyList(),
    val linkedGearIds: Set<Long> = emptySet(),
    val allGears: List<GearResponse> = emptyList(),
    val buffyImages: Map<BrawlStarsImageType, BrawlStarsImageEntity?> = emptyMap(),
)
