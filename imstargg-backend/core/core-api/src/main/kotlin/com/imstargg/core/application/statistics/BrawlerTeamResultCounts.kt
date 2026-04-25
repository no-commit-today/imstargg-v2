package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.BrawlerTeamResultCount

data class BrawlerTeamResultCounts(
    val items: List<BrawlerTeamResultCount>,
    val totalBattleCount: Long,
    val totalElementCount: Long,
)
