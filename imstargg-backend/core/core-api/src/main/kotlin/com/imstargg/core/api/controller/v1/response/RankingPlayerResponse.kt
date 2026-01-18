package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.ranking.RankingPlayer

data class RankingPlayerResponse(
    val tag: String,
    val name: String,
    val nameColor: String?,
    val iconId: Long,
    val clubName: String,
) {
    companion object {
        fun of(player: RankingPlayer) =
            RankingPlayerResponse(
                tag = player.tag.value,
                name = player.name,
                nameColor = player.nameColor,
                iconId = player.iconId.value,
                clubName = player.clubName,
            )
    }
}
