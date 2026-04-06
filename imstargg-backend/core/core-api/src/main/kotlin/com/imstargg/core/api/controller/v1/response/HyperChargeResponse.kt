package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.HyperCharge

data class HyperChargeResponse(
    val id: Long,
    val name: MessagesResponse,
    val imagePath: String?,
) {
    companion object {
        fun of(hyperCharge: HyperCharge) =
            HyperChargeResponse(
                id = hyperCharge.id.value,
                name = MessagesResponse.of(hyperCharge.name),
                imagePath = hyperCharge.imagePath,
            )
    }
}
