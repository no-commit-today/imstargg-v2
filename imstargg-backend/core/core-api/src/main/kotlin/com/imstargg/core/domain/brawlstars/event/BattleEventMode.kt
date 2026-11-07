package com.imstargg.core.domain.brawlstars.event

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.enums.BattleEventModeResultType

data class BattleEventMode(
    val id: BrawlStarsId,
    val name: Messages,
    val iconImageUrl: String?,
    /** 통계 수집 유형. 통계를 모으지 않는 모드는 본래 결과형이어도 null. */
    val resultType: BattleEventModeResultType?,
)
