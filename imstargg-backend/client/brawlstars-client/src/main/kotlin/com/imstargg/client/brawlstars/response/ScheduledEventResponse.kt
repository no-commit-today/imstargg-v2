package com.imstargg.client.brawlstars.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

data class ScheduledEventResponse(
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd'T'HHmmss.SSSX")
    val startTime: Instant,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd'T'HHmmss.SSSX")
    val endTime: Instant,
    val slotId: Int,
    val event: ScheduledEventLocationResponse,
)
