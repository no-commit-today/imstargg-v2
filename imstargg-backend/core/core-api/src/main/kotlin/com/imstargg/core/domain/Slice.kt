package com.imstargg.core.domain

data class Slice<T>(
    val content: List<T>,
    val hasNext: Boolean,
) {
    fun <R> map(transform: (T) -> R): Slice<R> =
        Slice(
            content = content.map(transform),
            hasNext = hasNext,
        )
}
