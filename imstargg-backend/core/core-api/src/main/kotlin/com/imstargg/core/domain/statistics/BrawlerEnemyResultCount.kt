package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerEnemyResultCount(
    val brawlerId: BrawlStarsId,
    val enemyBrawlerId: BrawlStarsId,
    val resultCount: ResultCount,
)
