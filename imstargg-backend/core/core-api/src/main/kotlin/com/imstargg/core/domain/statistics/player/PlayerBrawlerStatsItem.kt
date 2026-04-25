package com.imstargg.core.domain.statistics.player

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.ResultCount

data class PlayerBrawlerStatsItem(
    val brawlerId: BrawlStarsId,
    val resultCount: ResultCount,
)
