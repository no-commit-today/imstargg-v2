package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.player.PlayerBrawler
import com.imstargg.core.enums.BuffyType

data class PlayerBrawlerResponse(
    val brawler: BrawlerResponse?,
    val power: Int,
    val rank: Int,
    val trophies: Int,
    val highestTrophies: Int,
    val prestigeLevel: Int,
    val buffies: Set<BuffyType>,
) {
    companion object {
        fun of(playerBrawler: PlayerBrawler) =
            PlayerBrawlerResponse(
                brawler = playerBrawler.brawler?.let { BrawlerResponse.of(it) },
                power = playerBrawler.power,
                rank = playerBrawler.rank,
                trophies = playerBrawler.trophies,
                highestTrophies = playerBrawler.highestTrophies,
                prestigeLevel = playerBrawler.prestigeLevel,
                buffies = playerBrawler.buffies,
            )
    }
}
