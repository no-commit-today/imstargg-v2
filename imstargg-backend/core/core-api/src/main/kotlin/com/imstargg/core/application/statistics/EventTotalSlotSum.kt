package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class EventTotalSlotSum(
    val eventId: BrawlStarsId,
    val totalSlotCount: Long,
)
