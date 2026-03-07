package com.imstargg.core.domain.statistics

data class StarPlayerCount(
    val count: Long,
) {
    fun starPlayerRate(totalBattles: Long) = if (totalBattles > 0) count.toDouble() / totalBattles else 0.0

    fun merge(other: StarPlayerCount) = StarPlayerCount(count + other.count)
}
