package com.imstargg.storage.db.core.brawlstars.battle

data class BattleEntityTeamPlayer(
    val brawlStarsTag: String,
    val name: String,
    val brawler: BattleEntityTeamPlayerBrawler,
)
