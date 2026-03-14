package com.imstargg.infrastructure.db.core.statistics.battle

data class StatsBattleEntityTeamPlayer(
    val brawlStarsTag: String,
    val name: String,
    val brawler: StatsBattleEntityTeamPlayerBrawler,
)
