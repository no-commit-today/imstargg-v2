package com.imstargg.core.domain

data class Page<T>(
    val content: List<T>,
    val totalElementCount: Long,
) {
    fun <R> map(transform: (T) -> R): Page<R> =
        Page(
            content = content.map(transform),
            totalElementCount = totalElementCount,
        )
}
