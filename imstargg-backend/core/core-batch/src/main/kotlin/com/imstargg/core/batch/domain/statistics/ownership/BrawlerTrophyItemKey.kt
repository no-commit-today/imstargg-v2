package com.imstargg.core.batch.domain.statistics.ownership

import com.imstargg.core.enums.BrawlerTrophyRange

internal data class BrawlerTrophyItemKey(
    val brawlerBrawlStarsId: Long,
    val trophyRange: BrawlerTrophyRange,
    val itemBrawlStarsId: Long,
) {
    fun toBrawlerTrophyKey() =
        BrawlerTrophyKey(
            brawlerBrawlStarsId = brawlerBrawlStarsId,
            trophyRange = trophyRange,
        )
}
