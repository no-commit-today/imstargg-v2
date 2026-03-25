package com.imstargg.infrastructure.db.core.statistics

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Embeddable
data class RankCountsEmbedded(
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rank_counts", columnDefinition = "json", nullable = false)
    val counts: List<Long>,
) {
    init {
        require(counts.isNotEmpty()) { "counts는 비어 있을 수 없습니다." }
        require(counts.size <= MAX_RANK) { "counts 크기(${counts.size})는 최대 $MAX_RANK 이어야 합니다." }
    }

    companion object {
        private const val MAX_RANK = 10

        fun fromMap(map: Map<Int, Long>): RankCountsEmbedded {
            require(map.isNotEmpty()) { "map은 비어 있을 수 없습니다." }
            val maxRank = map.keys.max()
            val counts = (1..maxRank).map { rank -> map[rank] ?: 0L }
            return RankCountsEmbedded(counts)
        }
    }

    val size: Int get() = counts.size

    fun countOf(rank: Int): Long {
        require(rank in 1..counts.size) { "rank($rank)는 1~${counts.size} 범위여야 합니다." }
        return counts[rank - 1]
    }

    fun toMap(): Map<Int, Long> = counts.mapIndexed { index, value -> (index + 1) to value }.toMap()

    fun totalCount(): Long = counts.sum()

    operator fun plus(other: RankCountsEmbedded): RankCountsEmbedded {
        require(counts.size == other.counts.size) {
            "counts 크기가 다릅니다: ${counts.size} vs ${other.counts.size}"
        }
        return RankCountsEmbedded(counts.zip(other.counts) { a, b -> a + b })
    }
}
