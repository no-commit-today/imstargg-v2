package com.imstargg.core.domain.brawlstars.player

import com.imstargg.core.domain.brawlstars.brawler.Buffy

data class PlayerBrawlerBuffies(
    val gadget: PlayerBrawlerItem<Buffy>,
    val starPower: PlayerBrawlerItem<Buffy>,
    val hyperCharge: PlayerBrawlerItem<Buffy>,
)
