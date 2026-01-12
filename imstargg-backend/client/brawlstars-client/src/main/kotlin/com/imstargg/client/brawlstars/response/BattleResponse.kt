package com.imstargg.client.brawlstars.response

import java.time.OffsetDateTime

data class BattleResponse(
    val battleTime: OffsetDateTime,
    val event: EventResponse,
    val battle: BattleResultResponse,
)
