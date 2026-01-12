package com.imstargg.client.brawlstars.response

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
    val victories3vs3: Int,
    val soloVictories: Int,
    val duoVictories: Int,
    val bestRoboRumbleTime: Int,
    val bestTimeAsBigBrawler: Int,
    val club: PlayerClubResponse?,
    val brawlers: List<BrawlerStatResponse>,
)
