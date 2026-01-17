package com.imstargg.core.domain

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

data class OffsetLimit(
    val offset: Int,
    val limit: Int,
) {
    fun toPageable(): Pageable = PageRequest.of(offset / limit, limit)
}
