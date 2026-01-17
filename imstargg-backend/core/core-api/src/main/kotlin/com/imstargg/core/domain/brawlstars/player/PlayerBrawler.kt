package com.imstargg.core.domain.brawlstars.player

import com.imstargg.core.domain.brawlstars.brawler.Brawler

data class PlayerBrawler(
    val brawler: Brawler?,
    val power: Int,
    val rank: Int,
    val trophies: Int,
    val highestTrophies: Int,
)
