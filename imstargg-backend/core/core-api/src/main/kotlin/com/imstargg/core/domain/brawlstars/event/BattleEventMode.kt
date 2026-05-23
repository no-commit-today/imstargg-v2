package com.imstargg.core.domain.brawlstars.event

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.enums.BattleEventModeResultType

data class BattleEventMode(
    val id: BrawlStarsId,
    val name: Messages,
    val iconImageUrl: String?,
    val resultType: BattleEventModeResultType,
    /** 각 팀의 인원 수. 결과형 통계 픽률 계산의 분모(게임 수) 환산에 사용. */
    val playerCounts: List<Int>,
) {
    companion object {
        /** 절대다수의 모드가 3v3 이므로 mode 정보가 누락된 이벤트의 폴백 값. */
        val DEFAULT_PLAYER_COUNTS: List<Int> = listOf(3, 3)
    }
}
