package com.imstargg.core.batch.domain

class StarPlayerCountAccumulator<K> {
    private val counts = mutableMapOf<K, Long>()

    fun add(key: K) {
        counts.merge(key, 1L, Long::plus)
    }

    operator fun get(key: K): Long = counts[key] ?: 0
}
