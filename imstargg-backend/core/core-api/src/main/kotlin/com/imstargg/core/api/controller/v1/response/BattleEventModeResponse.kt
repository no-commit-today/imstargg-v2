package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.event.BattleEventMode
import com.imstargg.core.domain.statistics.StatsEventModeTypes
import com.imstargg.core.enums.BattleEventModeResultType

data class BattleEventModeResponse(
    val id: Long,
    val name: MessagesResponse,
    val iconImageUrl: String?,
    /** 통계 수집 유형. 통계를 모으지 않는 모드는 본래 결과형이어도 null. */
    val resultType: BattleEventModeResultType?,
) {
    companion object {
        /**
         * brawlstars 의 이벤트 모델은 통계를 모른다. 유형은 응답을 합치는 이곳에서 붙인다
         * (DOMAIN.md: 응답을 합치는 계층은 도메인 사이 의존으로 세지 않는다).
         */
        fun of(
            mode: BattleEventMode,
            statsEventModeTypes: StatsEventModeTypes,
        ) = BattleEventModeResponse(
            id = mode.id.value,
            name = MessagesResponse.of(mode.name),
            iconImageUrl = mode.iconImageUrl,
            resultType = statsEventModeTypes.resultTypeOf(mode.id),
        )
    }
}
