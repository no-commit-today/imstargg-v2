package com.imstargg.storage.db.core.brawlstars.battle

data class BattleEntityTeamPlayerBrawler(
    val brawlStarsId: Long,
    val name: String,
    val power: Int,
    val trophies: Int,
    val trophyChange: Int?,
)
