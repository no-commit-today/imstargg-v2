package com.imstargg.core.admin.request.statistics

import com.imstargg.core.enums.StatsEventModeType

data class StatsEventModeTypeUpdateRequest(
    /** 통계 수집 유형. null 이면 수집하지 않는다. */
    val type: StatsEventModeType?,
)
