package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.Buffies

data class BuffiesResponse(
    val gadget: BuffyResponse,
    val starPower: BuffyResponse,
    val hyperCharge: BuffyResponse,
) {
    companion object {
        fun of(buffies: Buffies) =
            BuffiesResponse(
                gadget = BuffyResponse(buffies.gadget.type, buffies.gadget.imageUrl),
                starPower = BuffyResponse(buffies.starPower.type, buffies.starPower.imageUrl),
                hyperCharge = BuffyResponse(buffies.hyperCharge.type, buffies.hyperCharge.imageUrl),
            )
    }
}
