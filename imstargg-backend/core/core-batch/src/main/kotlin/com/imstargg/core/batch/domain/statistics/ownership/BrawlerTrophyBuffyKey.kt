package com.imstargg.core.batch.domain.statistics.ownership

import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.core.enums.BuffyType

internal data class BrawlerTrophyBuffyKey(
    val brawlerBrawlStarsId: Long,
    val trophyRange: BrawlerTrophyRange,
    val buffyType: BuffyType,
) {
    fun toBrawlerTrophyKey() =
        BrawlerTrophyKey(
            brawlerBrawlStarsId = brawlerBrawlStarsId,
            trophyRange = trophyRange,
        )
}
