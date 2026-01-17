package com.imstargg.core.domain.brawlstars.ranking

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.Country

data class ClubRanking(
    val country: Country,
    val tag: BrawlStarsTag,
    val name: String,
    val badgeId: BrawlStarsId,
    val trophies: Int,
    val rank: Int,
    val memberCount: Int,
)
