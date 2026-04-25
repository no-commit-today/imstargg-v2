package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount

data class BrawlerEnemyResultCounts(
    val items: List<BrawlerEnemyResultCount>,
    val totalBattleCount: Long,
    val totalElementCount: Long,
)
