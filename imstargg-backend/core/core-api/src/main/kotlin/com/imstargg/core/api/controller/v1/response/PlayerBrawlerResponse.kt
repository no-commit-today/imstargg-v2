package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.player.PlayerBrawler

data class PlayerBrawlerResponse(
    val brawler: BrawlerResponse?,
    val power: Int,
    val rank: Int,
    val trophies: Int,
    val highestTrophies: Int,
) {
    companion object {
        fun of(playerBrawler: PlayerBrawler) =
            PlayerBrawlerResponse(
                brawler = playerBrawler.brawler?.let { BrawlerResponse.of(it) },
                power = playerBrawler.power,
                rank = playerBrawler.rank,
                trophies = playerBrawler.trophies,
                highestTrophies = playerBrawler.highestTrophies,
            )
    }
}
