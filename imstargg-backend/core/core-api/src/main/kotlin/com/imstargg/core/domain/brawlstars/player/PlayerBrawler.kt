package com.imstargg.core.domain.brawlstars.player

import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.enums.BuffyType

data class PlayerBrawler(
    val brawler: Brawler?,
    val power: Int,
    val rank: Int,
    val trophies: Int,
    val highestTrophies: Int,
    val prestigeLevel: Int,
    val buffies: Set<BuffyType>,
)
