package com.imstargg.core.api.controller

import com.imstargg.core.domain.OffsetLimit

data class OffsetLimitRequest(
    val offset: Int = 0,
    val limit: Int = 25,
) {
    fun toOffsetLimit(): OffsetLimit = OffsetLimit(offset, limit)
}
