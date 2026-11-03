package com.imstargg.core.admin.response

import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.Language

data class BattleEventModeResponse(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val names: Map<Language, String>,
    val iconImageUrl: String?,
    /** 통계 수집 유형. 통계를 모으지 않는 모드는 본래 결과형이어도 null. */
    val resultType: BattleEventModeResultType?,
)
