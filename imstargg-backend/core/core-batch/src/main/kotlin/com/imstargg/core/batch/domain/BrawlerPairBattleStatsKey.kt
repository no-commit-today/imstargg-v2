package com.imstargg.core.batch.domain

import com.imstargg.core.enums.StatsTierRange

data class BrawlerPairBattleStatsKey(
    val brawlerBrawlStarsId: Long,
    val pairBrawlerBrawlStarsId: Long,
    val eventBrawlStarsId: Long,
    val tierRange: StatsTierRange,
)
