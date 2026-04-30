package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.core.enums.BuffyType

data class PlayerBrawlerOwnershipSnapshot(
    val playerBrawlerId: Long,
    val brawlerBrawlStarsId: Long,
    val trophies: Int,
    val gearBrawlStarsIds: List<Long>,
    val gadgetBrawlStarsIds: List<Long>,
    val starPowerBrawlStarsIds: List<Long>,
    val hyperChargeBrawlStarsIds: List<Long>,
    val buffyTypes: List<BuffyType>,
)
