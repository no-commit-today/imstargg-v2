package com.imstargg.client.brawlstars.response

data class ClubMemberResponse(
    val tag: String,
    val name: String,
    val nameColor: String,
    val role: String,
    val trophies: Int,
    val icon: PlayerIconResponse,
)
