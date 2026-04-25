package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.BrawlerTeamRankStatisticsItem

data class BrawlerTeamRankCounts(
    val items: List<BrawlerTeamRankStatisticsItem>,
    val totalBattleCount: Long,
    val totalElementCount: Long,
)
