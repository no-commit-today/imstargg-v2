package com.imstargg.core.domain.statistics

import com.imstargg.core.enums.BuffyType

data class BrawlerBuffyOwnershipRate(
    val buffyType: BuffyType,
    val ownerCount: Long,
    val totalCount: Long,
)
