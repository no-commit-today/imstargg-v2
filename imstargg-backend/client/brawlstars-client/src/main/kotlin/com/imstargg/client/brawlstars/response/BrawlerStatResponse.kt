package com.imstargg.client.brawlstars.response

data class BrawlerStatResponse(
    val id: Long,
    val name: String,
    val power: Int,
    val rank: Int,
    val trophies: Int,
    val highestTrophies: Int,
    val gears: List<GearStatResponse>,
    val starPowers: List<AccessoryResponse>,
    val gadgets: List<AccessoryResponse>,
)
