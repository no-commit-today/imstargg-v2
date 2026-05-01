package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole

data class BrawlerResponse(
    val id: Long,
    val name: MessagesResponse,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val gadgets: List<GadgetResponse>,
    val gears: List<GearResponse>,
    val starPowers: List<StarPowerResponse>,
    val hyperCharge: HyperChargeResponse?,
    val portraitImageUrl: String?,
    val buffies: BuffiesResponse?,
) {
    companion object {
        fun of(brawler: Brawler) =
            BrawlerResponse(
                id = brawler.id.value,
                name = MessagesResponse.of(brawler.name),
                rarity = brawler.rarity,
                role = brawler.role,
                gadgets = brawler.gadgets.map { GadgetResponse.of(it) },
                gears = brawler.gears.map { GearResponse.of(it) },
                starPowers = brawler.starPowers.map { StarPowerResponse.of(it) },
                hyperCharge = brawler.hyperCharge?.let { HyperChargeResponse.of(it) },
                portraitImageUrl = brawler.portraitImageUrl,
                buffies = brawler.buffies?.let { BuffiesResponse.of(it) },
            )
    }
}
