package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.player.PlayerBrawler

data class PlayerBrawlerResponse(
    val brawler: BrawlerResponse?,
    val power: Int,
    val rank: Int,
    val trophies: Int,
    val highestTrophies: Int,
    val prestigeLevel: Int,
    val gadgets: List<PlayerBrawlerItemResponse<GadgetResponse>>,
    val gears: List<PlayerBrawlerItemResponse<GearResponse>>,
    val starPowers: List<PlayerBrawlerItemResponse<StarPowerResponse>>,
    val hyperCharge: PlayerBrawlerItemResponse<HyperChargeResponse>?,
    val buffies: PlayerBrawlerBuffiesResponse?,
) {
    companion object {
        fun of(pb: PlayerBrawler) =
            PlayerBrawlerResponse(
                brawler = pb.brawler?.let { BrawlerResponse.of(it) },
                power = pb.power,
                rank = pb.rank,
                trophies = pb.trophies,
                highestTrophies = pb.highestTrophies,
                prestigeLevel = pb.prestigeLevel,
                gadgets = pb.gadgets.map { PlayerBrawlerItemResponse(GadgetResponse.of(it.item), it.owned) },
                gears = pb.gears.map { PlayerBrawlerItemResponse(GearResponse.of(it.item), it.owned) },
                starPowers = pb.starPowers.map { PlayerBrawlerItemResponse(StarPowerResponse.of(it.item), it.owned) },
                hyperCharge = pb.hyperCharge?.let { PlayerBrawlerItemResponse(HyperChargeResponse.of(it.item), it.owned) },
                buffies = pb.buffies?.let { PlayerBrawlerBuffiesResponse.of(it) },
            )
    }
}
