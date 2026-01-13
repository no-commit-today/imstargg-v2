package com.imstargg.client.brawlstars.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

data class BattleResponse(
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd'T'HHmmss.SSSX")
    val battleTime: Instant,
    val event: EventResponse,
    val battle: BattleResultResponse,
)
