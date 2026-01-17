package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.player.Club
import com.imstargg.core.enums.ClubType

data class ClubResponse(
    val tag: String,
    val name: String,
    val description: String,
    val type: ClubType?,
    val badgeId: Long,
    val requiredTrophies: Int,
    val trophies: Int,
    val members: List<ClubMemberResponse>,
) {
    companion object {
        fun of(club: Club) =
            ClubResponse(
                tag = club.tag.value,
                name = club.name,
                description = club.description,
                type = club.type,
                badgeId = club.badgeId.value,
                requiredTrophies = club.requiredTrophies,
                trophies = club.trophies,
                members = club.members.map { ClubMemberResponse.of(it) },
            )
    }
}
