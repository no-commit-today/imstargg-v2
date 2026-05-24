package com.imstargg.core.domain

import org.springframework.data.domain.Slice as SpringDataSlice

data class Slice<T>(
    val content: List<T>,
    val hasNext: Boolean,
) {
    fun <R> map(transform: (T) -> R): Slice<R> =
        Slice(
            content = content.map(transform),
            hasNext = hasNext,
        )

    companion object {
        fun <T : Any> from(slice: SpringDataSlice<T>): Slice<T> =
            Slice(
                content = slice.content,
                hasNext = slice.hasNext(),
            )
    }
}
