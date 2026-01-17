package com.imstargg.core.worker.domain

import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.storage.db.core.brawlstars.player.PlayerEntity
import com.imstargg.storage.db.core.brawlstars.player.PlayerEntityClub

fun PlayerEntity.update(response: PlayerResponse) {
    update(
        name = response.name,
        nameColor = response.nameColor,
        iconBrawlStarsId = response.icon.id,
        trophies = response.trophies,
        highestTrophies = response.highestTrophies,
        expLevel = response.expLevel,
        expPoints = response.expPoints,
        qualifiedFromChampionshipChallenge = response.isQualifiedFromChampionshipChallenge,
        victories3vs3 = response.victories3vs3,
        victoriesSolo = response.victoriesSolo,
        victoriesDuo = response.victoriesDuo,
        bestRoboRumbleTime = response.bestRoboRumbleTime,
        bestTimeAsBigBrawler = response.bestTimeAsBigBrawler,
        club = response.club?.let { PlayerEntityClub(it.tag, it.name) },
    )
}
