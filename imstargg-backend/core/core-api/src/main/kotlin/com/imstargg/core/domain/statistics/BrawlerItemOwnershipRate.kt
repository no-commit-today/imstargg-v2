package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerItemOwnershipRate(
    val itemId: BrawlStarsId,
    val ownerCount: Long,
    val totalCount: Long,
)
