package com.imstargg.core.api.controller

import com.imstargg.core.domain.OffsetLimit
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class OffsetLimitRequest(
    @field:Min(0)
    @field:Max(10_000)
    val offset: Int = 0,
    @field:Min(1)
    @field:Max(100)
    val limit: Int = 25,
) {
    fun toOffsetLimit(): OffsetLimit = OffsetLimit(offset, limit)
}
