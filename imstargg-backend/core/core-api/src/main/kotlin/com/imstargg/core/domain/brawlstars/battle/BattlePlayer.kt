package com.imstargg.core.domain.brawlstars.battle

import com.imstargg.core.domain.brawlstars.BrawlStarsTag

data class BattlePlayer(
    val tag: BrawlStarsTag,
    val name: String,
    val brawler: BattlePlayerBrawler,
)
