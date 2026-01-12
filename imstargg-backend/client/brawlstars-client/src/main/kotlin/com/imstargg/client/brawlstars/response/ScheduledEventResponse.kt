package com.imstargg.client.brawlstars.response

import java.time.OffsetDateTime

data class ScheduledEventResponse(
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val slotId: Int,
    val event: ScheduledEventLocationResponse,
)
