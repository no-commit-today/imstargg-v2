package com.imstargg.core.batch.domain

import com.imstargg.core.enums.StatsTierRange

data class BrawlerEnemyBattleStatsKey(
    val brawlerBrawlStarsId: Long,
    val enemyBrawlerBrawlStarsId: Long,
    val eventBrawlStarsId: Long,
    val tierRange: StatsTierRange,
)
