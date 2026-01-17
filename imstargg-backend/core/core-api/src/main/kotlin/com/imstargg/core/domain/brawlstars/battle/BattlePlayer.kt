package com.imstargg.core.domain.brawlstars.battle

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.SoloRankTier

data class BattlePlayer(
    val tag: BrawlStarsTag,
    val name: String,
    val soloRankTier: SoloRankTier?,
    val brawler: BattlePlayerBrawler,
)
