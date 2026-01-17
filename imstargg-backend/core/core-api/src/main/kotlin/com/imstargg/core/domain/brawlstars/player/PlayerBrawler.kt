package com.imstargg.core.domain.brawlstars.player

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class PlayerBrawler(
    val brawlerId: BrawlStarsId,
    val power: Int,
    val rank: Int,
    val trophies: Int,
    val highestTrophies: Int,
)
