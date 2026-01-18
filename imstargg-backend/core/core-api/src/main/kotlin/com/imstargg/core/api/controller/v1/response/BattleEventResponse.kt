package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.event.Event
import com.imstargg.core.enums.BattleEventMode

data class BattleEventResponse(
    val id: Long,
    val mode: BattleEventMode?,
    val mapName: MessagesResponse,
) {
    companion object {
        fun of(event: Event) =
            BattleEventResponse(
                id = event.id.value,
                mode = event.mode,
                mapName = MessagesResponse.of(event.mapName),
            )
    }
}
