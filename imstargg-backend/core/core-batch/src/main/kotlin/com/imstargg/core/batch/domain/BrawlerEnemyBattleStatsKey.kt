package com.imstargg.core.batch.domain

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

data class BrawlerEnemyBattleStatsKey(
    val brawlerBrawlStarsId: Long,
    val enemyBrawlerBrawlStarsId: Long,
    val eventBrawlStarsId: Long,
    val tierRange: StatsTierRange,
    val battleDate: LocalDate,
)
