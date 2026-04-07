package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.Gadget

data class GadgetResponse(
    val id: Long,
    val name: MessagesResponse,
    val imageUrl: String?,
) {
    companion object {
        fun of(gadget: Gadget) =
            GadgetResponse(
                id = gadget.id.value,
                name = MessagesResponse.of(gadget.name),
                imageUrl = gadget.imageUrl,
            )
    }
}
