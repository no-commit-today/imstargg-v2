package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.player.Player

data class PlayerResponse(
    val tag: String,
    val name: String,
    val nameColor: String?,
    val iconId: Long,
    val trophies: Int,
    val highestTrophies: Int,
    val expLevel: Int,
    val expPoints: Int,
    val qualifiedFromChampionshipChallenge: Boolean,
    val victories3vs3: Int,
    val victoriesSolo: Int,
    val victoriesDuo: Int,
    val bestRoboRumbleTime: Int,
    val bestTimeAsBigBrawler: Int,
    val totalPrestigeLevel: Int,
    val club: PlayerClubResponse?,
) {
    companion object {
        fun of(player: Player) =
            PlayerResponse(
                tag = player.tag.value,
                name = player.name,
                nameColor = player.nameColor,
                iconId = player.iconId.value,
                trophies = player.trophies,
                highestTrophies = player.highestTrophies,
                expLevel = player.expLevel,
                expPoints = player.expPoints,
                qualifiedFromChampionshipChallenge = player.qualifiedFromChampionshipChallenge,
                victories3vs3 = player.victories3vs3,
                victoriesSolo = player.victoriesSolo,
                victoriesDuo = player.victoriesDuo,
                bestRoboRumbleTime = player.bestRoboRumbleTime,
                bestTimeAsBigBrawler = player.bestTimeAsBigBrawler,
                totalPrestigeLevel = player.totalPrestigeLevel,
                club = player.club?.let { PlayerClubResponse.of(it) },
            )
    }
}
