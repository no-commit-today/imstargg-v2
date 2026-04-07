package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.StarPower

data class StarPowerResponse(
    val id: Long,
    val name: MessagesResponse,
    val imageUrl: String?,
) {
    companion object {
        fun of(starPower: StarPower) =
            StarPowerResponse(
                id = starPower.id.value,
                name = MessagesResponse.of(starPower.name),
                imageUrl = starPower.imageUrl,
            )
    }
}
