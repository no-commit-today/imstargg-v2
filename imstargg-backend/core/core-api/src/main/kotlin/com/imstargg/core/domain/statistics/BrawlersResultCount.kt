package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlersResultCount(
    val brawlerIds: List<BrawlStarsId>,
    val resultCount: ResultCount,
)
