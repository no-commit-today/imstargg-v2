package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.event.BattleEventMode
import com.imstargg.core.enums.BattleEventModeResultType

data class BattleEventModeResponse(
    val id: Long,
    val name: MessagesResponse,
    val iconImageUrl: String?,
    /** 통계 수집 유형. 통계를 모으지 않는 모드는 본래 결과형이어도 null. */
    val resultType: BattleEventModeResultType?,
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
