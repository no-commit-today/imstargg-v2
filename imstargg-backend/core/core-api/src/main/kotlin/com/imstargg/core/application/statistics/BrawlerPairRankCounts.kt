package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.BrawlerPairRankStatisticsItem

data class BrawlerPairRankCounts(
    val items: List<BrawlerPairRankStatisticsItem>,
    val totalBattleCount: Long,
    val totalElementCount: Long,
)
