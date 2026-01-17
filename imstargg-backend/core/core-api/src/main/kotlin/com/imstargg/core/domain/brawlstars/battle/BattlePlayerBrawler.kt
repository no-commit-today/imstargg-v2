package com.imstargg.core.domain.brawlstars.battle

import com.imstargg.core.domain.brawlstars.brawler.Brawler

data class BattlePlayerBrawler(
    val brawler: Brawler?,
    val power: Int,
    val trophies: Int?,
    val trophyChange: Int?,
)
