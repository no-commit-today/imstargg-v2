package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityTeamPlayer

data class StatsBattleEntityTeamPlayer(
    val brawlStarsTag: String,
    val name: String,
    val brawler: StatsBattleEntityTeamPlayerBrawler,
) {
    companion object {
        fun from(player: BattleEntityTeamPlayer) =
            StatsBattleEntityTeamPlayer(
                brawlStarsTag = player.brawlStarsTag,
                name = player.name,
                brawler = StatsBattleEntityTeamPlayerBrawler.from(player.brawler),
            )
    }
}
