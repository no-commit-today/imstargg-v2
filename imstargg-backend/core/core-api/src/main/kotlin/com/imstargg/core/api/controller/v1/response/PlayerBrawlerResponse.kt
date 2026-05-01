package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.brawler.Buffy
import com.imstargg.core.domain.brawlstars.player.PlayerBrawler
import com.imstargg.core.enums.BuffyType

data class PlayerBrawlerResponse(
    val brawler: BrawlerResponse?,
    val power: Int,
    val rank: Int,
    val trophies: Int,
    val highestTrophies: Int,
    val prestigeLevel: Int,
    val gadgetBuffy: BuffyResponse?,
    val starPowerBuffy: BuffyResponse?,
    val hyperChargeBuffy: BuffyResponse?,
) {
    companion object {
        fun of(playerBrawler: PlayerBrawler): PlayerBrawlerResponse {
            val catalog = playerBrawler.brawler?.buffies
            return PlayerBrawlerResponse(
                brawler = playerBrawler.brawler?.let { BrawlerResponse.of(it) },
                power = playerBrawler.power,
                rank = playerBrawler.rank,
                trophies = playerBrawler.trophies,
                highestTrophies = playerBrawler.highestTrophies,
                prestigeLevel = playerBrawler.prestigeLevel,
                gadgetBuffy = resolveOwned(BuffyType.GADGET, playerBrawler.buffies, catalog?.gadget),
                starPowerBuffy = resolveOwned(BuffyType.STAR_POWER, playerBrawler.buffies, catalog?.starPower),
                hyperChargeBuffy = resolveOwned(BuffyType.HYPER_CHARGE, playerBrawler.buffies, catalog?.hyperCharge),
            )
        }

        private fun resolveOwned(
            type: BuffyType,
            owned: Set<BuffyType>,
            catalog: Buffy?,
        ): BuffyResponse? = if (type in owned) BuffyResponse(type, catalog?.imageUrl) else null
    }
}
