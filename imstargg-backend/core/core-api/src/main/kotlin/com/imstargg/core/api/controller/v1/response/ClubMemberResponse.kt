package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.player.ClubMember
import com.imstargg.core.enums.ClubMemberRole

data class ClubMemberResponse(
    val tag: String,
    val name: String,
    val nameColor: String?,
    val role: ClubMemberRole,
    val trophies: Int,
    val iconId: Long,
) {
    companion object {
        fun of(member: ClubMember) =
            ClubMemberResponse(
                tag = member.tag.value,
                name = member.name,
                nameColor = member.nameColor,
                role = member.role,
                trophies = member.trophies,
                iconId = member.iconId.value,
            )
    }
}
