package com.imstargg.core.admin.request.statistics

import com.fasterxml.jackson.annotation.JsonProperty
import com.imstargg.core.enums.StatsEventModeType

data class StatsEventModeTypeUpdateRequest(
    /** 통계 수집 유형. 키는 꼭 있어야 하고, null 이면 수집하지 않는다. */
    @param:JsonProperty(required = true)
    val type: StatsEventModeType?,
)
