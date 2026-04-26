package com.imstargg.core.domain.brawlstars

@ConsistentCopyVisibility
data class BrawlStarsIds private constructor(
    val values: List<BrawlStarsId>,
) {
    val size: Int get() = values.size

    fun isEmpty(): Boolean = values.isEmpty()

    fun key(): String = values.joinToString(DELIMITER) { it.value.toString() }

    companion object {
        const val DELIMITER = "_"

        fun of(ids: List<BrawlStarsId>): BrawlStarsIds = BrawlStarsIds(ids.sortedBy { it.value })
    }
}
