package com.imstargg.core.domain.brawlstars.player

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag

data class Player(
    val tag: BrawlStarsTag,
    val name: String,
    val nameColor: String?,
    val iconId: BrawlStarsId,
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
    val club: PlayerClub?,
)
