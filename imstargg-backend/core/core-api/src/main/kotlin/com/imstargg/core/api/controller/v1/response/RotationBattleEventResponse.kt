package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.event.RotationBattleEvent
import java.time.Instant

data class RotationBattleEventResponse(
    val event: BattleEventResponse,
    val startTime: Instant,
    val endTime: Instant,
) {
    companion object {
        fun of(rotationBattleEvent: RotationBattleEvent) =
            RotationBattleEventResponse(
                event = BattleEventResponse.of(rotationBattleEvent.event),
                startTime = rotationBattleEvent.startTime,
                endTime = rotationBattleEvent.endTime,
            )
    }
}
