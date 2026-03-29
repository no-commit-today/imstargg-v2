package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerResultCount(
    val brawlerId: BrawlStarsId,
    val resultCount: ResultCount,
    val starPlayerCount: Long,
)
