package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.event.RotationBattleEvent
import com.imstargg.core.domain.statistics.StatsEventModeTypes
import java.time.Instant

data class RotationBattleEventResponse(
    val event: BattleEventResponse,
    val startTime: Instant,
    val endTime: Instant,
) {
    companion object {
        fun of(
            rotationBattleEvent: RotationBattleEvent,
            statsEventModeTypes: StatsEventModeTypes,
        ) = RotationBattleEventResponse(
            event = BattleEventResponse.of(rotationBattleEvent.event, statsEventModeTypes),
            startTime = rotationBattleEvent.startTime,
            endTime = rotationBattleEvent.endTime,
        )
    }
}
