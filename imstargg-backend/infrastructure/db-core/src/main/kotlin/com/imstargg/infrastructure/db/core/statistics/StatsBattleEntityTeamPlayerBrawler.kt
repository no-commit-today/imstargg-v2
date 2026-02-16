package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntityBrawler

data class StatsBattleEntityTeamPlayerBrawler(
    val brawlStarsId: Long,
    val name: String,
    val power: Int,
    val trophies: Int,
) {
    companion object {
        fun from(brawler: BattleTeamPlayerEntityBrawler) =
            StatsBattleEntityTeamPlayerBrawler(
                brawlStarsId = brawler.brawlStarsId,
                name = brawler.name,
                power = brawler.power,
                trophies = brawler.trophies,
            )
    }
}
