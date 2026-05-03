package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole

data class BrawlerResponse(
    val id: Long,
    val name: MessagesResponse,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val portraitImageUrl: String?,
) {
    companion object {
        fun of(brawler: Brawler) =
            BrawlerResponse(
                id = brawler.id.value,
                name = MessagesResponse.of(brawler.name),
                rarity = brawler.rarity,
                role = brawler.role,
                portraitImageUrl = brawler.portraitImageUrl,
            )
    }
}
