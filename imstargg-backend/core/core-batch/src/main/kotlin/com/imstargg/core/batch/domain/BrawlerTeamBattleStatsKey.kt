package com.imstargg.core.batch.domain

import com.imstargg.core.enums.StatsTierRange

@ConsistentCopyVisibility
data class BrawlerTeamBattleStatsKey private constructor(
    val brawlerBrawlStarsIds: List<Long>,
    val eventBrawlStarsId: Long,
    val tierRange: StatsTierRange,
) {
    companion object {
        fun create(
            brawlerBrawlStarsIds: List<Long>,
            eventBrawlStarsId: Long,
            tierRange: StatsTierRange,
        ) = BrawlerTeamBattleStatsKey(
            brawlerBrawlStarsIds = brawlerBrawlStarsIds.sorted(),
            eventBrawlStarsId = eventBrawlStarsId,
            tierRange = tierRange,
        )
    }
}
