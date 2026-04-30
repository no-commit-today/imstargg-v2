package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.core.enums.BrawlerTrophyRange

internal data class BrawlerTrophyKey(
    val brawlerBrawlStarsId: Long,
    val trophyRange: BrawlerTrophyRange,
)
