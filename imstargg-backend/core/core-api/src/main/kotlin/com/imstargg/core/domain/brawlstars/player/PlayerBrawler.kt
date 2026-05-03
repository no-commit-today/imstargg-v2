package com.imstargg.core.domain.brawlstars.player

import com.imstargg.core.domain.brawlstars.brawler.Brawler
import com.imstargg.core.domain.brawlstars.brawler.Gadget
import com.imstargg.core.domain.brawlstars.brawler.Gear
import com.imstargg.core.domain.brawlstars.brawler.HyperCharge
import com.imstargg.core.domain.brawlstars.brawler.StarPower

data class PlayerBrawler(
    val brawler: Brawler?,
    val power: Int,
    val rank: Int,
    val trophies: Int,
    val highestTrophies: Int,
    val prestigeLevel: Int,
    val gadgets: List<PlayerBrawlerItem<Gadget>>,
    val gears: List<PlayerBrawlerItem<Gear>>,
    val starPowers: List<PlayerBrawlerItem<StarPower>>,
    val hyperCharge: PlayerBrawlerItem<HyperCharge>?,
    val buffies: PlayerBrawlerBuffies?,
)
