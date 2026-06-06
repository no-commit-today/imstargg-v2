package com.imstargg.core.batch.domain.statistics

import com.imstargg.infrastructure.db.core.statistics.RankCountsEmbedded

class RankCountAccumulator<K> {
    private val counts = mutableMapOf<K, RankCount>()

    fun add(
        key: K,
        rank: Int,
    ) {
        counts.getOrPut(key) { RankCount() }.add(rank)
    }

    fun <R> map(transform: (K, RankCountsEmbedded) -> R): List<R> = counts.map { (key, count) -> transform(key, count.toEmbedded()) }

    fun forEach(consumer: (K, RankCountsEmbedded) -> Unit) {
        counts.forEach { (key, count) ->
            consumer(key, count.toEmbedded())
        }
    }

    private class RankCount {
        private val ranks = mutableMapOf<Int, Long>()

        fun add(rank: Int) {
            ranks.merge(rank, 1L, Long::plus)
        }

        fun toEmbedded(): RankCountsEmbedded = RankCountsEmbedded.fromMap(ranks)
    }
}
