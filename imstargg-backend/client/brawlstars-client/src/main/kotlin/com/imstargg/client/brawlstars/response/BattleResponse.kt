package com.imstargg.client.brawlstars.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.OffsetDateTime

data class BattleResponse(
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd'T'HHmmss.SSSX")
    val battleTime: OffsetDateTime,
    val event: EventResponse,
    val battle: BattleResultResponse,
)
