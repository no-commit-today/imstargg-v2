package com.imstargg.infrastructure.db.core.statistics

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Embeddable
@ConsistentCopyVisibility
data class RankCountsEmbedded internal constructor(
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rank_counts", nullable = false)
    val counts: List<Long>,
    @Column(name = "total_count", nullable = false)
    val totalCount: Long,
) {
    init {
        require(counts.isNotEmpty()) { "counts는 비어 있을 수 없습니다." }
        require(counts.size <= MAX_RANK) { "counts 크기(${counts.size})는 최대 $MAX_RANK 이어야 합니다." }
    }

    companion object {
        const val MAX_RANK = 10

        fun fromMap(map: Map<Int, Long>): RankCountsEmbedded {
            require(map.isNotEmpty()) { "map은 비어 있을 수 없습니다." }
            val maxRank = map.keys.max()
            require(maxRank <= MAX_RANK) { "rank($maxRank)는 최대 $MAX_RANK 이어야 합니다." }
            val counts = (1..MAX_RANK).map { rank -> map[rank] ?: 0L }
            return RankCountsEmbedded(counts = counts, totalCount = counts.sum())
        }
    }

    val size: Int get() = counts.size

    fun countOf(rank: Int): Long {
        require(rank in 1..MAX_RANK) { "rank($rank)는 1~$MAX_RANK 범위여야 합니다." }
        return counts.getOrElse(rank - 1) { 0L }
    }

    fun toMap(): Map<Int, Long> = paddedCounts().mapIndexed { index, value -> (index + 1) to value }.toMap()

    fun weightedRankSum(): Long = paddedCounts().mapIndexed { index, count -> (index + 1).toLong() * count }.sum()

    operator fun plus(other: RankCountsEmbedded): RankCountsEmbedded =
        RankCountsEmbedded(
            counts = paddedCounts().zip(other.paddedCounts()) { a, b -> a + b },
            totalCount = totalCount + other.totalCount,
        )

    private fun paddedCounts(): List<Long> =
        if (counts.size == MAX_RANK) {
            counts
        } else {
            counts + List(MAX_RANK - counts.size) { 0L }
        }
}
