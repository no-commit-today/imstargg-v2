package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.BrawlerPairResultCount

data class BrawlerPairResultCounts(
    val items: List<BrawlerPairResultCount>,
    val totalBattleCount: Long,
    val totalElementCount: Long,
)
