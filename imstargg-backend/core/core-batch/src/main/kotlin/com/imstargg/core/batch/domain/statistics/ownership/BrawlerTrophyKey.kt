package com.imstargg.core.batch.domain.statistics.ownership

import com.imstargg.core.enums.BrawlerTrophyRange

internal data class BrawlerTrophyKey(
    val brawlerBrawlStarsId: Long,
    val trophyRange: BrawlerTrophyRange,
)
