package com.imstargg.core.api.controller

import com.imstargg.core.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val totalElementCount: Long,
) {
    companion object {
        fun <T> from(page: Page<T>): PageResponse<T> =
            PageResponse(
                content = page.content,
                totalElementCount = page.totalElementCount,
            )
    }
}
