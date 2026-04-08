package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerTeamResultCount(
    val brawlerIds: List<BrawlStarsId>,
    val resultCount: ResultCount,
)
