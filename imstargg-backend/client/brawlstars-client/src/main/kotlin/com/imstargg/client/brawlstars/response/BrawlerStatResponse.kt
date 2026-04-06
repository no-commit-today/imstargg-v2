package com.imstargg.client.brawlstars.response

data class BrawlerStatResponse(
    val id: Long,
    val name: String,
    val power: Int,
    val rank: Int,
    val trophies: Int,
    val highestTrophies: Int,
    val prestigeLevel: Int,
    val maxWinStreak: Int,
    val currentWinStreak: Int,
    val buffies: BrawlerBuffiesResponse,
    val skin: SkinResponse,
    val gears: List<GearStatResponse>,
    val starPowers: List<AccessoryResponse>,
    val gadgets: List<AccessoryResponse>,
    val hyperCharges: List<AccessoryResponse>,
)
