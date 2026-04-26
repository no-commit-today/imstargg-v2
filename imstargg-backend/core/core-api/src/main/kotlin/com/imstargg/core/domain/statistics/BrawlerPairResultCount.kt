package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerPairResultCount(
    val brawlerId: BrawlStarsId,
    val pairBrawlerId: BrawlStarsId,
    val resultCount: ResultCount,
)
