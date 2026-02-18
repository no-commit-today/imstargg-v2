package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.event.BattleEventMode

data class BattleEventModeResponse(
    val id: Long,
    val name: MessagesResponse,
    val iconImagePath: String?,
) {
    companion object {
        fun of(mode: BattleEventMode) =
            BattleEventModeResponse(
                id = mode.id.value,
                name = MessagesResponse.of(mode.name),
                iconImagePath = mode.iconImagePath,
            )
    }
}
