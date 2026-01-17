package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.player.PlayerClub

data class PlayerClubResponse(
    val tag: String,
    val name: String,
) {
    companion object {
        fun of(club: PlayerClub) =
            PlayerClubResponse(
                tag = club.tag.value,
                name = club.name,
            )
    }
}
