package com.imstargg.core.domain.brawlstars.battle

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BattlePlayerBrawler(
    val id: BrawlStarsId,
    val name: String,
    val power: Int,
    val trophies: Int,
    val trophyChange: Int?,
)
