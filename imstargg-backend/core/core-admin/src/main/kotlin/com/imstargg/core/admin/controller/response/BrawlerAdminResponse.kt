package com.imstargg.core.admin.controller.response

import com.imstargg.core.admin.response.BrawlerResponse
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language

data class BrawlerAdminResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val hasImage: Boolean,
) {
    companion object {
        fun from(response: BrawlerResponse) =
            BrawlerAdminResponse(
                brawlStarsId = response.brawler.brawlStarsId,
                names = response.names,
                rarity = response.brawler.rarity,
                role = response.brawler.role,
                hasImage = response.image != null,
            )
    }
}
