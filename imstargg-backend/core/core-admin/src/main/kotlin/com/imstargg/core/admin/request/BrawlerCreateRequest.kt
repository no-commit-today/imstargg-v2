package com.imstargg.core.admin.request

import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole

data class BrawlerCreateRequest(
    val brawlStarsId: Long,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
)
