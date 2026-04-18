package com.imstargg.core.admin.response

import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language

data class BrawlerResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val imageUrl: String?,
)
