package com.imstargg.core.admin.response

import com.imstargg.core.enums.Language
import com.imstargg.core.enums.StatsEventModeType

data class BattleEventModeResponse(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val names: Map<Language, String>,
    val iconImageUrl: String?,
    /** 통계 수집 유형. 없으면 수집하지 않는다. 응답을 합치는 컨트롤러가 채운다. */
    val statsType: StatsEventModeType? = null,
)
