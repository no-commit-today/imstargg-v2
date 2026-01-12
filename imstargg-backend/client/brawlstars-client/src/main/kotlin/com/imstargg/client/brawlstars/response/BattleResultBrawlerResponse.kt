package com.imstargg.client.brawlstars.response

data class BattleResultBrawlerResponse(
    val id: Long,
    val name: String,
    val power: Int,
    val trophies: Int,
    val trophyChange: Int?,
)
