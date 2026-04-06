package com.imstargg.client.brawlstars.response

import com.fasterxml.jackson.annotation.JsonProperty

data class PlayerResponse(
    val tag: String,
    val name: String,
    val nameColor: String,
    val icon: PlayerIconResponse,
    val trophies: Int,
    val highestTrophies: Int,
    val expLevel: Int,
    val expPoints: Int,
    val isQualifiedFromChampionshipChallenge: Boolean,
    @field:JsonProperty("3vs3Victories")
    val victories3vs3: Int,
    @field:JsonProperty("soloVictories")
    val victoriesSolo: Int,
    @field:JsonProperty("duoVictories")
    val victoriesDuo: Int,
    val bestRoboRumbleTime: Int,
    val bestTimeAsBigBrawler: Int,
    val totalPrestigeLevel: Int,
    val club: PlayerClubResponse?,
    val brawlers: List<BrawlerStatResponse>,
)
