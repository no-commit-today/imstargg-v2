package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerItemOwnershipRate
import com.imstargg.core.enums.BrawlerTrophyRange
import org.springframework.stereotype.Service

@Service
class BrawlerItemOwnershipStatisticsService(
    private val repository: BrawlerItemOwnershipStatisticsRepository,
) {
    fun getGearOwnership(
        brawlerId: BrawlStarsId,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerItemOwnershipRate> =
        repository.findGearOwnership(
            brawlerId = brawlerId,
            trophyRange = trophyRange,
        )

    fun getGadgetOwnership(
        brawlerId: BrawlStarsId,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerItemOwnershipRate> =
        repository.findGadgetOwnership(
            brawlerId = brawlerId,
            trophyRange = trophyRange,
        )

    fun getStarPowerOwnership(
        brawlerId: BrawlStarsId,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerItemOwnershipRate> =
        repository.findStarPowerOwnership(
            brawlerId = brawlerId,
            trophyRange = trophyRange,
        )

    fun getHyperChargeOwnership(
        brawlerId: BrawlStarsId,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerItemOwnershipRate> =
        repository.findHyperChargeOwnership(
            brawlerId = brawlerId,
            trophyRange = trophyRange,
        )
}
