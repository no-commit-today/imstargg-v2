package com.imstargg.client.brawlstars.response

data class ClubRankingResponse(
    val tag: String,
    val name: String,
    val badgeId: Int,
    val trophies: Int,
    val rank: Int,
    val memberCount: Int,
)
