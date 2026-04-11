package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.Page

data class BrawlerTeamRankPageStatistics(
    val totalBattleCount: Long,
    val items: Page<BrawlerTeamRankStatistics>,
)
