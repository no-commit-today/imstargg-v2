package com.imstargg.client.brawlstars.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.OffsetDateTime

data class ScheduledEventResponse(
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd'T'HHmmss.SSSX")
    val startTime: OffsetDateTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd'T'HHmmss.SSSX")
    val endTime: OffsetDateTime,
    val slotId: Int,
    val event: ScheduledEventLocationResponse,
)
