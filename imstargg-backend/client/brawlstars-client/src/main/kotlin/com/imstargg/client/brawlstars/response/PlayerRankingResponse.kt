package com.imstargg.client.brawlstars.response

data class PlayerRankingResponse(
    val tag: String,
    val name: String,
    val nameColor: String,
    val icon: PlayerIconResponse,
    val trophies: Int,
    val rank: Int,
    val club: PlayerRankingClubResponse?,
)
