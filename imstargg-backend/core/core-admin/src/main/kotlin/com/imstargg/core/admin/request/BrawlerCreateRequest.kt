package com.imstargg.core.admin.request

import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language

data class BrawlerCreateRequest(
    val brawlStarsId: Long,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val names: Map<Language, String> = emptyMap(),
    val gearBrawlStarsIds: Set<Long> = emptySet(),
)
