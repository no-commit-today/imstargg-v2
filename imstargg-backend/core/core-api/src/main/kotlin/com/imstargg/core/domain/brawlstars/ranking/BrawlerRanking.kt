package com.imstargg.core.domain.brawlstars.ranking

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.Country

data class BrawlerRanking(
    val country: Country,
    val brawlerId: BrawlStarsId,
    val rank: Int,
    val player: RankingPlayer,
    val trophies: Int,
    val rankChange: Int?,
)
