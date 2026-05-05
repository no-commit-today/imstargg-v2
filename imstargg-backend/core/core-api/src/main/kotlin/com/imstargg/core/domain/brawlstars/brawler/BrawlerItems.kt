package com.imstargg.core.domain.brawlstars.brawler

data class BrawlerItems(
    val gadgets: List<Gadget>,
    val gears: List<Gear>,
    val starPowers: List<StarPower>,
    val hyperCharge: HyperCharge?,
    val buffies: Buffies?,
)
