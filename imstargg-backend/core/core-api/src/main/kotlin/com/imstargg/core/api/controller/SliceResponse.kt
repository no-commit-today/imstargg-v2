package com.imstargg.core.api.controller

import com.imstargg.core.domain.Slice

data class SliceResponse<T>(
    val content: List<T>,
    val hasNext: Boolean,
) {
    companion object {
        fun <T> from(slice: Slice<T>): SliceResponse<T> =
            SliceResponse(
                content = slice.content,
                hasNext = slice.hasNext,
            )
    }
}
