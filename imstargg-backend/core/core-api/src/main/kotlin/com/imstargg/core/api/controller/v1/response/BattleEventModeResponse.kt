package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.event.BattleEventMode
import com.imstargg.core.enums.BattleEventResultType

data class BattleEventModeResponse(
    val id: Long,
    val name: MessagesResponse,
    val iconImageUrl: String?,
    val resultType: BattleEventResultType,
) {
    companion object {
        fun of(mode: BattleEventMode) =
            BattleEventModeResponse(
                id = mode.id.value,
                name = MessagesResponse.of(mode.name),
                iconImageUrl = mode.iconImageUrl,
                resultType = mode.resultType,
            )
    }
}
