package com.imstargg.core.admin.controller.response

import com.imstargg.core.admin.response.GearResponse
import com.imstargg.core.enums.GearRarity
import com.imstargg.core.enums.Language

data class GearAdminResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val rarity: GearRarity,
    val hasImage: Boolean,
) {
    companion object {
        fun from(response: GearResponse) =
            GearAdminResponse(
                brawlStarsId = response.gear.brawlStarsId,
                names = response.names,
                rarity = response.gear.rarity,
                hasImage = response.image != null,
            )
    }
}
