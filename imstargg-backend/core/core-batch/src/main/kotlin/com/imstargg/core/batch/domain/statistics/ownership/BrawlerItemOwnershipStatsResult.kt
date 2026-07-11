package com.imstargg.core.batch.domain.statistics.ownership

import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerBuffyOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGadgetOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGearOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerHyperChargeOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerStarPowerOwnershipStatsEntity

data class BrawlerItemOwnershipStatsResult(
    val gears: List<BrawlerGearOwnershipStatsEntity>,
    val gadgets: List<BrawlerGadgetOwnershipStatsEntity>,
    val starPowers: List<BrawlerStarPowerOwnershipStatsEntity>,
    val hyperCharges: List<BrawlerHyperChargeOwnershipStatsEntity>,
    val buffies: List<BrawlerBuffyOwnershipStatsEntity>,
) {
    val totalEntityCount: Int
        get() = gears.size + gadgets.size + starPowers.size + hyperCharges.size + buffies.size
}
