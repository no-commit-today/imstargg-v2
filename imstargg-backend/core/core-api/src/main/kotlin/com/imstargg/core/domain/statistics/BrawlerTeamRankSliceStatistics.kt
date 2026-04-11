package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.Slice

data class BrawlerTeamRankSliceStatistics(
    val totalBattleCount: Long,
    val items: Slice<BrawlerTeamRankStatistics>,
)
