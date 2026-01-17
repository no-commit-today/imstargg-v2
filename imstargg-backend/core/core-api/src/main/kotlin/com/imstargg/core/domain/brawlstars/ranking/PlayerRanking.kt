package com.imstargg.core.domain.brawlstars.ranking

import com.imstargg.core.enums.Country

data class PlayerRanking(
    val country: Country,
    val rank: Int,
    val player: RankingPlayer,
    val trophies: Int,
    val rankChange: Int?,
)
