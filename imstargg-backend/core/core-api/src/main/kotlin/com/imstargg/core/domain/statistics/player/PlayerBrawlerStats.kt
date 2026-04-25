package com.imstargg.core.domain.statistics.player

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.BattleType
import java.time.LocalDate

data class PlayerBrawlerStats(
    val playerTag: BrawlStarsTag,
    val battleType: BattleType,
    val startBattleDate: LocalDate,
    val endBattleDate: LocalDate,
    val items: List<PlayerBrawlerStatsItem>,
)
