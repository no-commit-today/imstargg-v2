package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.battle.BattlePlayerBrawler

data class BattlePlayerBrawlerResponse(
    val brawler: BrawlerResponse?,
    val power: Int,
    val trophies: Int?,
    val trophyChange: Int?,
) {
    companion object {
        fun of(brawler: BattlePlayerBrawler) =
            BattlePlayerBrawlerResponse(
                brawler = brawler.brawler?.let { BrawlerResponse.of(it) },
                power = brawler.power,
                trophies = brawler.trophies,
                trophyChange = brawler.trophyChange,
            )
    }
}
