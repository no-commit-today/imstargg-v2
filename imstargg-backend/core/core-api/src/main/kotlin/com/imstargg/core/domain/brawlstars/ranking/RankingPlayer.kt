package com.imstargg.core.domain.brawlstars.ranking

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag

data class RankingPlayer(
    val tag: BrawlStarsTag,
    val name: String,
    val nameColor: String?,
    val iconId: BrawlStarsId,
    val clubName: String,
)
