package com.imstargg.core.domain.brawlstars.battle

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.BattleMode
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import java.time.OffsetDateTime

data class Battle(
    val battleTime: OffsetDateTime,
    val event: BattleEvent,
    val mode: BattleMode?,
    val type: BattleType?,
    val result: BattleResult?,
    val duration: Int?,
    val starPlayerTag: BrawlStarsTag?,
    val rank: Int?,
    val trophyChange: Int?,
    val teams: List<List<BattlePlayer>>,
)
