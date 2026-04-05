package com.imstargg.core.batch.domain

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

data class BrawlerTeamBattleStatsKey private constructor(
    val brawlerBrawlStarsIds: List<Long>,
    val eventBrawlStarsId: Long,
    val tierRange: StatsTierRange,
    val battleDate: LocalDate,
) {
    companion object {
        fun create(
            brawlerBrawlStarsIds: List<Long>,
            eventBrawlStarsId: Long,
            tierRange: StatsTierRange,
            battleDate: LocalDate,
        ) = BrawlerTeamBattleStatsKey(
            brawlerBrawlStarsIds = brawlerBrawlStarsIds.sorted(),
            eventBrawlStarsId = eventBrawlStarsId,
            tierRange = tierRange,
            battleDate = battleDate,
        )
    }
}
