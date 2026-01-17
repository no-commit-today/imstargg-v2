package com.imstargg.infrastructure.db.core.brawlstars.battle

data class BattleEntityTeamPlayer(
    val brawlStarsTag: String,
    val name: String,
    val brawler: BattleEntityTeamPlayerBrawler,
)
