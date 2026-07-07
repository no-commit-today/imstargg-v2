package com.imstargg.core.domain.statistics

data class RankCount(
    val counts: Map<Int, Long>,
) {
    val totalCount: Long get() = counts.values.sum()

    val averageRank: Double
        get() =
            if (totalCount > 0) {
                counts.entries.sumOf { it.key.toLong() * it.value }.toDouble() / totalCount
            } else {
                0.0
            }

    fun merge(other: RankCount): RankCount {
        val merged = counts.toMutableMap()
        other.counts.forEach { (rank, count) -> merged.merge(rank, count, Long::plus) }
        return RankCount(merged)
    }
}
