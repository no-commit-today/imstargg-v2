package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.BrawlerItems

data class BrawlerItemsResponse(
    val gadgets: List<GadgetResponse>,
    val gears: List<GearResponse>,
    val starPowers: List<StarPowerResponse>,
    val hyperCharge: HyperChargeResponse?,
    val buffies: BuffiesResponse?,
) {
    companion object {
        fun of(items: BrawlerItems) =
            BrawlerItemsResponse(
                gadgets = items.gadgets.map { GadgetResponse.of(it) },
                gears = items.gears.map { GearResponse.of(it) },
                starPowers = items.starPowers.map { StarPowerResponse.of(it) },
                hyperCharge = items.hyperCharge?.let { HyperChargeResponse.of(it) },
                buffies = items.buffies?.let { BuffiesResponse.of(it) },
            )
    }
}
