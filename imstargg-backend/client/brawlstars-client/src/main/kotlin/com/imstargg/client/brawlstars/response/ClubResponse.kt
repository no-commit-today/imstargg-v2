package com.imstargg.client.brawlstars.response

data class ClubResponse(
    val tag: String,
    val name: String,
    val description: String,
    val type: String,
    val badgeId: Long,
    val requiredTrophies: Int,
    val trophies: Int,
    val members: List<ClubMemberResponse>,
)
