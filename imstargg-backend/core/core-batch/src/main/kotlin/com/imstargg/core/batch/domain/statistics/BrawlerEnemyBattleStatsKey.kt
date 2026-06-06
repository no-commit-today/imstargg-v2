package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.StatsTierRange

data class BrawlerEnemyBattleStatsKey(
    val brawlerBrawlStarsId: Long,
    val enemyBrawlerBrawlStarsId: Long,
    val eventBrawlStarsId: Long,
    val tierRange: StatsTierRange,
)
