package com.imstargg.core.domain.brawlstars.event

import java.time.Instant

data class RotationBattleEvent(
    val event: BattleEvent,
    val startTime: Instant,
    val endTime: Instant,
)
