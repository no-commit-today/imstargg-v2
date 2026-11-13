package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.event.BattleEvent
import com.imstargg.core.domain.statistics.StatsEventModeTypes

data class BattleEventResponse(
    val id: Long,
    val mode: BattleEventModeResponse?,
    val mapName: MessagesResponse,
) {
    companion object {
        fun of(
            event: BattleEvent,
            statsEventModeTypes: StatsEventModeTypes,
        ) = BattleEventResponse(
            id = event.id.value,
            mode = event.mode?.let { BattleEventModeResponse.of(it, statsEventModeTypes) },
            mapName = MessagesResponse.of(event.mapName),
        )
    }
}
