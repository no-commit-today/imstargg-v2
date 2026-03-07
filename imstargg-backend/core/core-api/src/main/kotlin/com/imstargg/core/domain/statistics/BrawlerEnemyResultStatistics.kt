package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerEnemyResultStatistics(
    val brawlerId: BrawlStarsId,
    val enemyBrawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val winRate: Double,
)
