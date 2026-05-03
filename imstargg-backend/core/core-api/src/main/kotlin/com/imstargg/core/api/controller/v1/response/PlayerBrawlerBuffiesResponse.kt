package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.player.PlayerBrawlerBuffies

data class PlayerBrawlerBuffiesResponse(
    val gadget: PlayerBrawlerItemResponse<BuffyResponse>,
    val starPower: PlayerBrawlerItemResponse<BuffyResponse>,
    val hyperCharge: PlayerBrawlerItemResponse<BuffyResponse>,
) {
    companion object {
        fun of(buffies: PlayerBrawlerBuffies) =
            PlayerBrawlerBuffiesResponse(
                gadget =
                    PlayerBrawlerItemResponse(
                        item = BuffyResponse(buffies.gadget.item.type, buffies.gadget.item.imageUrl),
                        owned = buffies.gadget.owned,
                    ),
                starPower =
                    PlayerBrawlerItemResponse(
                        item = BuffyResponse(buffies.starPower.item.type, buffies.starPower.item.imageUrl),
                        owned = buffies.starPower.owned,
                    ),
                hyperCharge =
                    PlayerBrawlerItemResponse(
                        item = BuffyResponse(buffies.hyperCharge.item.type, buffies.hyperCharge.item.imageUrl),
                        owned = buffies.hyperCharge.owned,
                    ),
            )
    }
}
