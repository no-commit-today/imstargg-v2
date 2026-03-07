package com.imstargg.core.domain.brawlstars.event

import java.time.OffsetDateTime

data class RotationBattleEvent(
    val event: BattleEvent,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
)
