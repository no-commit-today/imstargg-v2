package com.imstargg.core.application.statistics

data class RawCounts<T>(
    val items: List<T>,
    val totalElementCount: Long,
)
