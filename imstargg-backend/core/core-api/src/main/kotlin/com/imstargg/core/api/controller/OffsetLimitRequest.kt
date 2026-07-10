package com.imstargg.core.api.controller

import com.imstargg.core.domain.OffsetLimit
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class OffsetLimitRequest(
    /** offset 상한 10,000 = limit 최대(100) 기준 100페이지. 공개 API 의 딥 오프셋 쿼리 방어 목적. */
    @field:Min(0)
    @field:Max(10_000)
    val offset: Int = 0,
    @field:Min(1)
    @field:Max(100)
    val limit: Int = 25,
) {
    fun toOffsetLimit(): OffsetLimit = OffsetLimit(offset, limit)
}
