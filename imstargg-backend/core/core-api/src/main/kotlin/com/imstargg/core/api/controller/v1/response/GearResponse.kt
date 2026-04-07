package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.Gear
import com.imstargg.core.enums.GearRarity

data class GearResponse(
    val id: Long,
    val rarity: GearRarity,
    val name: MessagesResponse,
    val imageUrl: String?,
) {
    companion object {
        fun of(gear: Gear) =
            GearResponse(
                id = gear.id.value,
                rarity = gear.rarity,
                name = MessagesResponse.of(gear.name),
                imageUrl = gear.imageUrl,
            )
    }
}
