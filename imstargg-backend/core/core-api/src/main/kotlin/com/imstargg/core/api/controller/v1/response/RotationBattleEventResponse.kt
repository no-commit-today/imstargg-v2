package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.event.RotationBattleEvent
import java.time.OffsetDateTime

data class RotationBattleEventResponse(
    val event: BattleEventResponse,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
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
