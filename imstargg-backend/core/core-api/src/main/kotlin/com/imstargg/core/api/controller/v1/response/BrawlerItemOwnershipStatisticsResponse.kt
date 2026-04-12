package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.statistics.BrawlerBuffyOwnershipRate
import com.imstargg.core.domain.statistics.BrawlerItemOwnershipRate

data class BrawlerItemOwnershipStatisticsItemResponse(
    val itemId: Long,
    val ownershipRate: Double,
    val ownerCount: Long,
    val totalCount: Long,
) {
    companion object {
        fun of(rate: BrawlerItemOwnershipRate) =
            BrawlerItemOwnershipStatisticsItemResponse(
                itemId = rate.itemId.value,
                ownershipRate = if (rate.totalCount > 0) rate.ownerCount.toDouble() / rate.totalCount else 0.0,
                ownerCount = rate.ownerCount,
                totalCount = rate.totalCount,
            )
    }
}

data class BrawlerBuffyOwnershipStatisticsItemResponse(
    val buffyType: String,
    val ownershipRate: Double,
    val ownerCount: Long,
    val totalCount: Long,
) {
    companion object {
        fun of(rate: BrawlerBuffyOwnershipRate) =
            BrawlerBuffyOwnershipStatisticsItemResponse(
                buffyType = rate.buffyType.name,
                ownershipRate = if (rate.totalCount > 0) rate.ownerCount.toDouble() / rate.totalCount else 0.0,
                ownerCount = rate.ownerCount,
                totalCount = rate.totalCount,
            )
    }
}

data class BrawlerItemOwnershipStatisticsResponse(
    val gears: List<BrawlerItemOwnershipStatisticsItemResponse>,
    val gadgets: List<BrawlerItemOwnershipStatisticsItemResponse>,
    val starPowers: List<BrawlerItemOwnershipStatisticsItemResponse>,
    val hyperCharges: List<BrawlerItemOwnershipStatisticsItemResponse>,
    val buffies: List<BrawlerBuffyOwnershipStatisticsItemResponse>,
)
